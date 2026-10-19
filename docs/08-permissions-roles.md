# 08 — Rôles & permissions

Système maison (pas de LuckPerms) pour garder un seul modèle de données, la propagation Redis et l'UI intégrée.
Schéma : tables `roles`, `role_permissions`, `player_roles`, `player_permissions` (cf. `03-database.md`).

## 1. Concepts

- **Rôle** : clé, nom, préfixe (MiniMessage), couleur du pseudo, **poids**, parent (héritage simple), `is_default`, `is_staff`.
- **Nœud** : `nexus.<domaine>.<action>[.<détail>]`, jokers `nexus.admin.*` et `*`, négation via `value = false`.
- **Contexte** : `*` (partout), `server:<type>` (`server:lobby`), `server:<id>`, `mode:<modeId>` (actif dans une instance de ce mode).
- **Attribution temporaire** : `expires_at` sur `player_roles` / `player_permissions` (VIP 30 jours, etc.). Un job de l'orchestrateur purge toutes les minutes et publie `perm.player-changed`.

## 2. Résolution (algorithme unique, dans `nexus-common` → testé une fois, utilisé partout)

```
entrées : joueur, contexte courant {serverType, serverId, modeId?}
1. rôles = rôles non expirés du joueur ∪ {rôle par défaut}
2. pour chaque rôle, remonter les parents → liste ordonnée (enfant avant parent)
3. sources par priorité décroissante :
     a. permissions directes du joueur
     b. rôles triés par poids décroissant, chaque rôle avant ses parents
4. pour un nœud demandé N, parcourir les sources dans l'ordre ; dans une source, la règle applicable la plus spécifique gagne :
     - correspondance exacte > joker le plus long (nexus.admin.config.* > nexus.admin.*) > '*'
     - à spécificité égale : contexte le plus spécifique (server:<id> > server:<type>/mode:<id> > '*')
   la première source qui a une règle applicable décide (true/false)
5. aucune règle → false (sauf permissions Bukkit avec default: true déclarées par des plugins tiers)
```

- Résultat mis en cache par joueur sous forme de `PermissionTree` (trie des nœuds) → `has(node)` en O(longueur du nœud).
- Rôle **d'affichage** = rôle de plus haut poids (préfixe, couleur, tri dans la tab list).

## 3. Application

| Plateforme | Mécanisme |
|---|---|
| **Paper** | `PermissionAttachment` par joueur, recalculé au join et sur `perm.*` : les nœuds **explicites** sont posés tels quels ; les **jokers sont développés** contre (a) le catalogue Nexus (`Permissions`), (b) les permissions enregistrées dans Bukkit (`PluginManager#getPermissions`), (c) les nœuds vanilla `minecraft.command.*`. Le code Nexus appelle `PermissionService.has(uuid, node)` (jokers natifs, sans dépendre du développement). Aucune injection de `Permissible` (interdit : réflexion sur internals). Commandes Brigadier : `requires(src -> perms.has(src, node))`. |
| **Velocity** | `PermissionsSetupEvent` → `PermissionProvider` qui délègue au même `PermissionTree`. |
| **Orchestrateur** | n'en a pas besoin (il ne reçoit pas de commandes joueurs). Les actions sensibles reçues par Redis sont vérifiées **sur le serveur émetteur**. |

Propagation : modification → transaction BDD → `perm.roles-changed {roleIds}` ou `perm.player-changed {playerId}` → chaque serveur/proxy recharge le cache concerné et recalcule les attachments des joueurs connectés (main thread, coût négligeable). Cible : < 2 s (NFR-05).

## 4. Commandes

```
/role list
/role info <rôle>
/role create <clé> <nom>
/role delete <clé>                         (confirmation Dialog)
/role set <clé> prefix|color|weight|parent <valeur>
/role perm add <rôle> <nœud> [true|false] [contexte]
/role perm remove <rôle> <nœud> [contexte]
/role grant <joueur> <rôle> [durée: 30d, 12h…]
/role revoke <joueur> <rôle>
/perm check <joueur> <nœud>                 (affiche la décision ET la règle qui l'a prise — indispensable au debug)
/perm player add|remove <joueur> <nœud> [valeur] [contexte] [durée]
```

Toutes ces actions sont aussi dans **Hub admin → Rôles** (cf. `07`). Garde-fou : on ne peut attribuer/modifier qu'un rôle de **poids strictement inférieur** au sien (sauf `*`).

## 5. Catalogue des nœuds (à tenir à jour avec `fr.nexus.common.Permissions`)

| Nœud | Description | Défaut (seed) |
|---|---|---|
| `nexus.play` | rejoindre les files | joueur |
| `nexus.party.use` | utiliser les parties | joueur |
| `nexus.party.size.<n>` | taille max de party (défaut 4) | vip : 8 |
| `nexus.friends.use` | amis | joueur |
| `nexus.msg` | messages privés | joueur |
| `nexus.stats.view` / `nexus.stats.view.others` | voir ses stats / celles des autres | joueur / joueur |
| `nexus.leaderboard.view` | leaderboards | joueur |
| `nexus.chat.color` | MiniMessage limité dans le chat | vip |
| `nexus.game.create.private` | créer une partie privée | vip |
| `nexus.game.create` | créer une partie (tous paramètres) | admin |
| `nexus.instance.list` | voir les parties en cours | helper |
| `nexus.instance.start` / `.stop` / `.kick` | gérer une instance | admin / admin / modérateur |
| `nexus.spectate` | observer n'importe quelle partie | helper |
| `nexus.tp` | téléportation staff | modérateur |
| `nexus.player.lookup` | fiche joueur | modérateur |
| `nexus.staff.chat` | chat staff | helper |
| `nexus.admin.hub` | ouvrir le Hub admin | admin |
| `nexus.admin.network` | drainer/arrêter des serveurs | admin |
| `nexus.config.edit` / `.publish` / `.rollback` | configs de jeu | admin |
| `nexus.map.create` / `.edit` / `.publish` / `.disable` | maps | builder (create/edit), admin (publish/disable) |
| `nexus.server.editor.join` | accès au serveur editor | builder |
| `nexus.mode.toggle` | activer/désactiver modes et variantes | admin |
| `nexus.role.manage` / `nexus.role.grant` | gérer les rôles / attribuer | owner / admin |
| `nexus.bypass.full` | rejoindre une instance pleine | admin |
