# 03 — Base de données & état partagé

Source de vérité du schéma : `common/nexus-data/src/main/resources/db/migration/`.
Ce document explique **pourquoi** le schéma est ainsi et **comment** l'utiliser. Le mettre à jour à chaque migration.

## 1. Répartition Postgres / Redis / mémoire

| Donnée | Où | Durée de vie | Pourquoi |
|---|---|---|---|
| Profil joueur, rôles, permissions, amis | Postgres + cache Caffeine (5 min, invalidé par message) | durable | référentiel |
| Configs de jeu, maps | Postgres + cache local par serveur (invalidé par `config.updated` / `map.published`) | durable, versionné | rollback, audit |
| Matchs, participants, stats, records | Postgres | durable | leaderboards, historique |
| Présence (proxy, serveur, instance) | Redis `presence:*` (TTL 60 s) | éphémère | changement fréquent |
| Party, invitations | Redis | éphémère (TTL) | pas d'intérêt à persister |
| Registre serveurs & instances | Redis (écrit par l'orchestrateur) | éphémère | état temps réel |
| Files d'attente | **Mémoire de l'orchestrateur** (+ snapshot Redis pour reprise par le standby) | éphémère | latence |
| Réservations de place | Redis `reservation:*` (TTL 15 s) | éphémère | validation à la connexion |
| Top N leaderboards | Redis ZSET `lb:*` (recalcul périodique) | cache | lecture très fréquente (hologrammes) |
| État de jeu (scores, lits, ressources) | **Mémoire de l'instance** uniquement | durée de la partie | performance |

## 2. Modèle (Postgres)

```mermaid
erDiagram
    players ||--o{ player_name_history : a
    players ||--o{ player_roles : possède
    roles ||--o{ player_roles : attribué
    roles ||--o{ role_permissions : contient
    roles ||--o| roles : hérite
    players ||--o{ player_permissions : a
    players ||--o{ friendships : ami
    players ||--o{ friend_requests : demande
    game_modes ||--o{ game_variants : propose
    game_modes ||--o{ game_configs : configure
    game_modes ||--o{ maps : utilise
    maps ||--o{ map_versions : versions
    game_modes ||--o{ matches : joué
    matches ||--o{ match_participants : inclut
    players ||--o{ match_participants : participe
    players ||--o{ player_stats : agrège
    players ||--o{ player_records : détient
```

### Points de conception

- **`players.id` = UUID Mojang** (serveur premium). Le nom est dénormalisé et mis à jour à chaque connexion (historique dans `player_name_history`).
- **`settings` en JSONB** : préférences évolutives sans migration (clés documentées dans `fr.nexus.common.model.PlayerSettings`).
- **Rôles** : arbre via `parent_id` (un seul parent, pour garder une résolution simple). Un seul rôle `is_default`. Résolution détaillée dans `08-permissions-roles.md`.
- **`context`** des permissions : `*`, `server:<type>`, `server:<id>`, `mode:<modeId>`. Non-null (défaut `*`) pour pouvoir être en clé primaire.
- **`friendships`** : une ligne par paire avec `player_a < player_b` (contrainte CHECK). Requête « mes amis » = `WHERE player_a = ? UNION ALL WHERE player_b = ?`.
- **Configs versionnées** : chaque sauvegarde crée une **nouvelle ligne** (`version+1`) ; une seule `is_active` par (mode, preset) grâce à l'index unique partiel. Rollback = activer une ancienne version. Les parties enregistrent `config_id` utilisé → traçabilité.
- **Maps** : `maps` = identité/rotation ; `map_versions` = chaque publication (template + marqueurs JSON + checksum). Les instances chargent `current_version`.
- **`matches.id` en UUIDv7** : généré par le serveur de jeu dès le démarrage → sert de **clé d'idempotence** du rapport (insert `ON CONFLICT DO NOTHING` → si le match existe déjà, le rapport a déjà été appliqué).
- **`ranked = false`** pour les parties privées, forcées sous le minimum ou abandonnées : elles sont historisées mais **n'alimentent pas** `player_stats`.

### Stats : un modèle générique « clé/valeur agrégée »

`player_stats(player_id, mode_id, variant_id, period, stat_key, value)`

- Chaque jeu **déclare** ses `StatKey` avec une agrégation : `SUM` (kills, wins), `MAX` (meilleure série), `MIN` (meilleur temps), `SET` (série en cours).
- Stats **automatiques** (écrites par le core pour tous les jeux) : `games_played`, `wins`, `losses`, `draws`, `playtime_s`, `win_streak` (SET), `best_win_streak` (MAX).
- À la fin d'un match classé, pour chaque participant, le core upserte chaque stat sur **2 variantes** (`<variant>` et `*`) × **3 périodes** (`all`, `m:YYYY-MM`, `w:YYYY-Www` ISO) (+ `s:<saison>` si une saison est active).
- Upserts **en batch** par type d'agrégation :

```sql
-- SUM
INSERT INTO player_stats (player_id, mode_id, variant_id, period, stat_key, value)
VALUES (?, ?, ?, ?, ?, ?), ...
ON CONFLICT (player_id, mode_id, variant_id, period, stat_key)
DO UPDATE SET value = player_stats.value + EXCLUDED.value, updated_at = now();
-- MAX : value = GREATEST(player_stats.value, EXCLUDED.value)
-- MIN : value = LEAST(player_stats.value, EXCLUDED.value)
-- SET : value = EXCLUDED.value
```

- Leaderboard = `SELECT player_id, value FROM player_stats WHERE mode_id=? AND variant_id=? AND period=? AND stat_key=? ORDER BY value DESC LIMIT 100` (index `idx_player_stats_leaderboard`). Pour `MIN`, `ORDER BY value ASC` (le sens est porté par la `StatKey`).
- Ratio K/D, % de victoires : **calculés à l'affichage**, jamais stockés.

### Records

`player_records` stocke un record **avec la preuve** (`match_id`, date) — ex. Dropper `best_time:<mapKey>`. Mise à jour seulement si meilleur (`WHERE` sur le sens). Leaderboard par map via `idx_player_records_board`.

### Volume & rétention

| Table | Croissance estimée (200 joueurs simultanés) | Stratégie |
|---|---|---|
| `match_participants` | ~50 k lignes/jour | OK jusqu'à quelques dizaines de millions ; partitionnement mensuel envisagé en v2 |
| `player_stats` | ~50 lignes par joueur et par mode joué par semaine | Purge des périodes `w:*` de plus de 12 semaines, `m:*` de plus de 24 mois (job orchestrateur) |
| `audit_log` | faible | Rétention 1 an |

## 3. Transactions types

**Rapport de match** (une transaction, isolation `READ COMMITTED`) :
1. `INSERT INTO matches ... ON CONFLICT (id) DO NOTHING RETURNING id` → si rien n'est retourné, **stop** (déjà appliqué).
2. `INSERT INTO match_participants` (batch).
3. Si `ranked` : upserts `player_stats` (batch par agrégation), puis records (`INSERT ... ON CONFLICT DO UPDATE ... WHERE <meilleur>`).
4. `COMMIT` → publier `match.finished` (après commit uniquement).

**Publication de config** : `UPDATE game_configs SET is_active = false WHERE mode_id=? AND preset_key=? AND is_active` puis `INSERT ... is_active = true` + `INSERT audit_log`, une transaction ; puis `config.updated`.

## 4. Clés Redis (état)

Préfixe global configurable (`nexus:` par défaut, omis ci-dessous). Toutes définies dans `RedisKeys`.

| Clé | Type | TTL | Écrite par | Contenu |
|---|---|---|---|---|
| `server:<serverId>` | HASH | 15 s (rafraîchi par heartbeat) | serveur | type, modes, players, maxInstances, instances, mspt, freeMemMb, state, address |
| `servers` | SET | — | orchestrateur | ids des serveurs vivants |
| `instance:<instanceId>` | HASH | — (supprimée au DESTROYED) | serveur hôte | modeId, variantId, mapKey, state, players, maxPlayers, flags, inviteCode, serverId |
| `instances:<modeId>:<variantId>:waiting` | ZSET | — | orchestrateur | instances joignables, score = nb joueurs (pour « la plus remplie d'abord ») |
| `presence:<uuid>` | HASH | 60 s (rafraîchi 20 s par le proxy) | proxy + serveur | proxyId, serverId, instanceId, name |
| `presence:online` | SET | — | proxy | UUIDs en ligne (comptage réseau) |
| `reservation:<instanceId>:<uuid>` | STRING | 15 s | orchestrateur | token, teamHint |
| `rejoin:<uuid>` | STRING | `rejoinWindowSeconds` | serveur hôte | instanceId |
| `party:<partyId>` | HASH | 6 h glissant | proxy | leader, members (JSON), createdAt, settings |
| `party:member:<uuid>` | STRING | 6 h glissant | proxy | partyId |
| `party:invite:<partyId>:<uuid>` | STRING | 60 s | proxy | inviterId |
| `queue:snapshot` | STRING (JSON) | 30 s | orchestrateur | état des files pour reprise par le standby |
| `orchestrator:leader` | STRING | 5 s (PX, renouvelé 1 s) | orchestrateur | id du leader |
| `lb:<mode>:<variant>:<period>:<stat>` | ZSET | 10 min | orchestrateur | top 100 (score = valeur ; MIN → score négatif) |
| `name:<uuid>` | STRING | 1 h | divers | nom (affichage des leaderboards) |
| `cooldown:<scope>:<uuid>` | STRING | variable | divers | anti-spam (invitations, demandes d'amis, /play) |

## 5. Caches locaux (Caffeine)

| Cache | Où | Clé → valeur | Expiration | Invalidation |
|---|---|---|---|---|
| `ProfileCache` | Paper | UUID → `PlayerProfile` | à la déconnexion + 10 min | `player.updated` |
| `RoleCache` | Paper, Velocity | roleId → `ResolvedRole` | aucune | `perm.roles-changed` (rechargement complet, quelques Ko) |
| `PermissionCache` | Paper, Velocity | UUID → permissions effectives | à la déconnexion | `perm.player-changed` |
| `ConfigCache` | Paper (game) | (mode, preset) → config typée | aucune | `config.updated` |
| `MapCache` | Paper (game) | (mode, mapKey) → `MapData` + template local | aucune | `map.published` |
| `NameCache` | partout | UUID → nom | 1 h | — |

## 6. Accès en dev

- `docker compose -f infra/docker-compose.dev.yml up -d postgres valkey` puis `psql postgres://nexus:nexus@localhost:5432/nexus`.
- `valkey-cli -a nexus MONITOR` pour voir le trafic.
