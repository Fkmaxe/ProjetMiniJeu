-- =====================================================================
-- Données par défaut : rôles, permissions, modes et variantes de départ
-- =====================================================================

INSERT INTO roles (key, display_name, prefix, color, weight, is_default, is_staff) VALUES
    ('joueur',     'Joueur',     '',                                          'gray',    0,   TRUE,  FALSE),
    ('vip',        'VIP',        '<gradient:#f7c948:#f08c00>VIP</gradient> ', '#f7c948', 10,  FALSE, FALSE),
    ('builder',    'Builder',    '<#3bc9db>Builder</#3bc9db> ',               '#3bc9db', 40,  FALSE, TRUE),
    ('helper',     'Helper',     '<#69db7c>Helper</#69db7c> ',                '#69db7c', 50,  FALSE, TRUE),
    ('moderateur', 'Modérateur', '<#4dabf7>Modo</#4dabf7> ',                  '#4dabf7', 60,  FALSE, TRUE),
    ('admin',      'Admin',      '<#ff6b6b>Admin</#ff6b6b> ',                 '#ff6b6b', 90,  FALSE, TRUE),
    ('owner',      'Fondateur',  '<#e03131><b>Fondateur</b></#e03131> ',      '#e03131', 100, FALSE, TRUE);

-- Héritage : vip <- joueur ; helper <- vip ; moderateur <- helper ; admin <- moderateur ; owner <- admin ; builder <- joueur
UPDATE roles SET parent_id = (SELECT id FROM roles WHERE key = 'joueur')     WHERE key IN ('vip', 'builder');
UPDATE roles SET parent_id = (SELECT id FROM roles WHERE key = 'vip')        WHERE key = 'helper';
UPDATE roles SET parent_id = (SELECT id FROM roles WHERE key = 'helper')     WHERE key = 'moderateur';
UPDATE roles SET parent_id = (SELECT id FROM roles WHERE key = 'moderateur') WHERE key = 'admin';
UPDATE roles SET parent_id = (SELECT id FROM roles WHERE key = 'admin')      WHERE key = 'owner';

INSERT INTO role_permissions (role_id, node, value) SELECT id, n, TRUE FROM roles, unnest(ARRAY[
    'nexus.play', 'nexus.party.use', 'nexus.friends.use', 'nexus.msg', 'nexus.stats.view', 'nexus.leaderboard.view'
]) AS n WHERE key = 'joueur';

INSERT INTO role_permissions (role_id, node, value) SELECT id, n, TRUE FROM roles, unnest(ARRAY[
    'nexus.party.size.8', 'nexus.game.create.private', 'nexus.chat.color'
]) AS n WHERE key = 'vip';

INSERT INTO role_permissions (role_id, node, value) SELECT id, n, TRUE FROM roles, unnest(ARRAY[
    'nexus.map.edit', 'nexus.map.create', 'nexus.server.editor.join'
]) AS n WHERE key = 'builder';

INSERT INTO role_permissions (role_id, node, value) SELECT id, n, TRUE FROM roles, unnest(ARRAY[
    'nexus.staff.chat', 'nexus.spectate', 'nexus.instance.list'
]) AS n WHERE key = 'helper';

INSERT INTO role_permissions (role_id, node, value) SELECT id, n, TRUE FROM roles, unnest(ARRAY[
    'nexus.player.lookup', 'nexus.instance.kick', 'nexus.tp'
]) AS n WHERE key = 'moderateur';

INSERT INTO role_permissions (role_id, node, value) SELECT id, n, TRUE FROM roles, unnest(ARRAY[
    'nexus.admin.*', 'nexus.game.*', 'nexus.instance.*', 'nexus.config.*', 'nexus.map.*', 'nexus.role.grant'
]) AS n WHERE key = 'admin';

INSERT INTO role_permissions (role_id, node, value) SELECT id, '*', TRUE FROM roles WHERE key = 'owner';

-- Modes et variantes de départ
INSERT INTO game_modes (id, display_name, sort_order, icon) VALUES
    ('dropper', 'Dropper', 10, 'minecraft:feather'),
    ('rush',    'Rush',    20, 'minecraft:red_bed'),
    ('bedwars', 'Bedwars', 30, 'minecraft:white_bed');

INSERT INTO game_variants (mode_id, id, display_name, team_count, team_size, min_players, min_waiting_instances, sort_order) VALUES
    ('dropper', 'solo',  'Course (solo)',  12, 1, 2, 1, 10),
    ('rush',    '2x4',   'Rush 4v4',        2, 4, 4, 1, 10),
    ('rush',    '4x2',   'Rush 4 équipes',  4, 2, 4, 0, 20),
    ('bedwars', '8x1',   'Solo',            8, 1, 4, 1, 10),
    ('bedwars', '8x2',   'Doubles',         8, 2, 6, 1, 20),
    ('bedwars', '4x3',   '3v3v3v3',         4, 3, 6, 0, 30),
    ('bedwars', '4x4',   '4v4v4v4',         4, 4, 8, 1, 40);
