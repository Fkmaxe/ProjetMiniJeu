-- =====================================================================
-- Nexus — schéma initial
-- Conventions : snake_case, timestamptz partout, ids joueurs = UUID Mojang,
-- ids de match = UUIDv7 générés par l'application.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Joueurs
-- ---------------------------------------------------------------------
CREATE TABLE players (
    id                UUID PRIMARY KEY,
    name              VARCHAR(16)  NOT NULL,
    locale            VARCHAR(10)  NOT NULL DEFAULT 'fr_FR',
    first_seen        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    playtime_seconds  BIGINT       NOT NULL DEFAULT 0,
    settings          JSONB        NOT NULL DEFAULT '{}'::jsonb   -- préférences (friend_requests, party_invites, chat, visibilité...)
);
CREATE INDEX idx_players_name_lower ON players (lower(name));

CREATE TABLE player_name_history (
    player_id   UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    name        VARCHAR(16) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, changed_at)
);

-- ---------------------------------------------------------------------
-- Rôles & permissions
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id            SERIAL PRIMARY KEY,
    key           VARCHAR(32)  NOT NULL UNIQUE,          -- 'admin', 'vip'...
    display_name  VARCHAR(64)  NOT NULL,
    prefix        VARCHAR(128) NOT NULL DEFAULT '',      -- MiniMessage
    color         VARCHAR(16)  NOT NULL DEFAULT 'gray',  -- couleur nom (MiniMessage / hex)
    weight        INT          NOT NULL DEFAULT 0,       -- plus élevé = plus prioritaire (affichage, résolution)
    parent_id     INT          NULL REFERENCES roles(id) ON DELETE SET NULL,
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,   -- attribué implicitement à tous
    is_staff      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_roles_single_default ON roles (is_default) WHERE is_default;

CREATE TABLE role_permissions (
    role_id   INT          NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    node      VARCHAR(128) NOT NULL,                     -- 'nexus.game.create', 'nexus.admin.*'
    value     BOOLEAN      NOT NULL DEFAULT TRUE,        -- FALSE = négation explicite
    context   VARCHAR(64)  NOT NULL DEFAULT '*',         -- '*' | 'server:lobby' | 'mode:bedwars'
    PRIMARY KEY (role_id, node, context)
);

CREATE TABLE player_roles (
    player_id   UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    role_id     INT         NOT NULL REFERENCES roles(id)   ON DELETE CASCADE,
    granted_by  UUID        NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NULL,
    PRIMARY KEY (player_id, role_id)
);
CREATE INDEX idx_player_roles_expiry ON player_roles (expires_at) WHERE expires_at IS NOT NULL;

CREATE TABLE player_permissions (
    player_id   UUID         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    node        VARCHAR(128) NOT NULL,
    value       BOOLEAN      NOT NULL DEFAULT TRUE,
    context     VARCHAR(64)  NOT NULL DEFAULT '*',
    expires_at  TIMESTAMPTZ  NULL,
    PRIMARY KEY (player_id, node, context)
);

-- ---------------------------------------------------------------------
-- Social
-- ---------------------------------------------------------------------
CREATE TABLE friendships (
    player_a  UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    player_b  UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    since     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_a, player_b),
    CHECK (player_a < player_b)                          -- une seule ligne par paire
);
CREATE INDEX idx_friendships_b ON friendships (player_b);

CREATE TABLE friend_requests (
    sender_id    UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    receiver_id  UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (sender_id, receiver_id),
    CHECK (sender_id <> receiver_id)
);
CREATE INDEX idx_friend_requests_receiver ON friend_requests (receiver_id);

-- ---------------------------------------------------------------------
-- Modes de jeu, variantes, configs
-- ---------------------------------------------------------------------
CREATE TABLE game_modes (
    id            VARCHAR(32)  PRIMARY KEY,              -- 'bedwars'
    display_name  VARCHAR(64)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INT          NOT NULL DEFAULT 0,
    icon          VARCHAR(64)  NOT NULL DEFAULT 'minecraft:paper',  -- clé d'item
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE game_variants (
    mode_id         VARCHAR(32) NOT NULL REFERENCES game_modes(id) ON DELETE CASCADE,
    id              VARCHAR(32) NOT NULL,                -- 'solo', '4x4'
    display_name    VARCHAR(64) NOT NULL,
    team_count      INT         NOT NULL CHECK (team_count >= 1),
    team_size       INT         NOT NULL CHECK (team_size >= 1),
    min_players     INT         NOT NULL CHECK (min_players >= 1),
    enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    queue_enabled   BOOLEAN     NOT NULL DEFAULT TRUE,   -- FALSE = uniquement à la demande
    min_waiting_instances INT   NOT NULL DEFAULT 1,      -- politique du mode autonome
    default_preset  VARCHAR(32) NOT NULL DEFAULT 'classic',
    sort_order      INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (mode_id, id)
);

CREATE TABLE game_configs (
    id              BIGSERIAL   PRIMARY KEY,
    mode_id         VARCHAR(32) NOT NULL REFERENCES game_modes(id) ON DELETE CASCADE,
    preset_key      VARCHAR(32) NOT NULL,                -- 'classic', 'fast', 'event-halloween'
    preset_name     VARCHAR(64) NOT NULL,
    version         INT         NOT NULL,
    schema_version  INT         NOT NULL,                -- version du schéma de config du jeu
    data            JSONB       NOT NULL,                -- valeurs (seulement celles ≠ défaut OU toutes : cf. doc 07)
    is_active       BOOLEAN     NOT NULL DEFAULT FALSE,  -- version en vigueur du preset
    comment         VARCHAR(256) NULL,
    created_by      UUID        NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (mode_id, preset_key, version)
);
CREATE UNIQUE INDEX uq_game_configs_active ON game_configs (mode_id, preset_key) WHERE is_active;

-- ---------------------------------------------------------------------
-- Maps
-- ---------------------------------------------------------------------
CREATE TABLE maps (
    id               BIGSERIAL   PRIMARY KEY,
    mode_id          VARCHAR(32) NOT NULL REFERENCES game_modes(id) ON DELETE CASCADE,
    key              VARCHAR(48) NOT NULL,               -- 'lighthouse'
    display_name     VARCHAR(64) NOT NULL,
    authors          TEXT[]      NOT NULL DEFAULT '{}',
    status           VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED')),
    variants         TEXT[]      NOT NULL DEFAULT '{}',  -- variantes compatibles ('4x4'...)
    weight           INT         NOT NULL DEFAULT 100,   -- probabilité relative dans la rotation
    current_version  INT         NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (mode_id, key)
);

CREATE TABLE map_versions (
    map_id           BIGINT       NOT NULL REFERENCES maps(id) ON DELETE CASCADE,
    version          INT          NOT NULL,
    template_uri     VARCHAR(512) NOT NULL,              -- 'file:///templates/bedwars/lighthouse/3.tar.zst' | 's3://...'
    template_sha256  CHAR(64)     NOT NULL,
    schema_version   INT          NOT NULL,              -- version du MapSchema du jeu
    data             JSONB        NOT NULL,              -- marqueurs (coordonnées relatives), régions, méta
    published_by     UUID         NULL,
    published_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (map_id, version)
);

-- ---------------------------------------------------------------------
-- Saisons (optionnel, prévu)
-- ---------------------------------------------------------------------
CREATE TABLE seasons (
    id         SERIAL      PRIMARY KEY,
    key        VARCHAR(32) NOT NULL UNIQUE,              -- '2026-s1'
    name       VARCHAR(64) NOT NULL,
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    CHECK (ends_at > starts_at)
);

-- ---------------------------------------------------------------------
-- Matches & stats
-- ---------------------------------------------------------------------
CREATE TABLE matches (
    id            UUID         PRIMARY KEY,              -- UUIDv7 (ordre temporel)
    mode_id       VARCHAR(32)  NOT NULL REFERENCES game_modes(id),
    variant_id    VARCHAR(32)  NOT NULL,
    map_id        BIGINT       NULL REFERENCES maps(id) ON DELETE SET NULL,
    map_version   INT          NULL,
    config_id     BIGINT       NULL REFERENCES game_configs(id) ON DELETE SET NULL,
    server_id     VARCHAR(48)  NOT NULL,
    instance_id   VARCHAR(48)  NOT NULL,
    is_private    BOOLEAN      NOT NULL DEFAULT FALSE,
    ranked        BOOLEAN      NOT NULL DEFAULT TRUE,    -- FALSE = ne compte pas dans les stats (privée, forcée, abandonnée)
    started_at    TIMESTAMPTZ  NOT NULL,
    ended_at      TIMESTAMPTZ  NOT NULL,
    duration_ms   BIGINT       NOT NULL,
    result_type   VARCHAR(16)  NOT NULL CHECK (result_type IN ('NORMAL', 'DRAW', 'ABORTED')),
    winner_team   VARCHAR(32)  NULL,
    metadata      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    FOREIGN KEY (mode_id, variant_id) REFERENCES game_variants(mode_id, id)
);
CREATE INDEX idx_matches_mode_ended ON matches (mode_id, variant_id, ended_at DESC);

CREATE TABLE match_participants (
    match_id   UUID        NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_id  UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    team       VARCHAR(32) NULL,
    placement  INT         NULL,                         -- 1 = vainqueur
    outcome    VARCHAR(8)  NOT NULL CHECK (outcome IN ('WIN', 'LOSS', 'DRAW', 'LEFT')),
    stats      JSONB       NOT NULL DEFAULT '{}'::jsonb, -- stats de la partie {"kills":4,"beds_broken":1}
    PRIMARY KEY (match_id, player_id)
);
CREATE INDEX idx_match_participants_player ON match_participants (player_id, match_id DESC);

-- Stats agrégées : une ligne par (joueur, mode, variante|'*', période, stat).
-- period : 'all' | 'm:2026-10' | 'w:2026-W43' | 's:<season_key>'
CREATE TABLE player_stats (
    player_id   UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    mode_id     VARCHAR(32) NOT NULL REFERENCES game_modes(id) ON DELETE CASCADE,
    variant_id  VARCHAR(32) NOT NULL,                    -- '*' = toutes variantes confondues
    period      VARCHAR(16) NOT NULL,
    stat_key    VARCHAR(48) NOT NULL,                    -- 'wins','losses','games_played','kills','beds_broken'...
    value       BIGINT      NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, mode_id, variant_id, period, stat_key)
);
-- Index de leaderboard : top N d'une stat pour un mode/variante/période
CREATE INDEX idx_player_stats_leaderboard
    ON player_stats (mode_id, variant_id, period, stat_key, value DESC);

-- Records personnels avec preuve (ex. meilleur temps Dropper par map)
CREATE TABLE player_records (
    player_id    UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    mode_id      VARCHAR(32) NOT NULL REFERENCES game_modes(id) ON DELETE CASCADE,
    record_key   VARCHAR(96) NOT NULL,                   -- 'best_time:lighthouse', 'best_time:all_levels'
    value        BIGINT      NOT NULL,                   -- ms, points... (sens défini par le jeu : LOWER/HIGHER is better)
    match_id     UUID        NULL REFERENCES matches(id) ON DELETE SET NULL,
    achieved_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, mode_id, record_key)
);
CREATE INDEX idx_player_records_board ON player_records (mode_id, record_key, value);

-- ---------------------------------------------------------------------
-- Journal d'audit (actions admin : configs, maps, rôles, instances forcées)
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    id         BIGSERIAL    PRIMARY KEY,
    actor_id   UUID         NULL,                        -- NULL = système / console
    action     VARCHAR(64)  NOT NULL,                    -- 'config.publish', 'role.grant', 'instance.force_stop'
    target     VARCHAR(128) NULL,
    data       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_created ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id, created_at DESC);
