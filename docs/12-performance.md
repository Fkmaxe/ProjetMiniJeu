# 12 — Performance

Objectif : maximiser le nombre de parties par cœur CPU sans dégrader le ressenti (NFR-01 à NFR-04).

## 1. Budgets

| Indicateur | Cible | Mesure |
|---|---|---|
| MSPT serveur de jeu (p95) | < 30 ms | heartbeat (`mspt`, `msptP95`) + spark |
| MSPT d'une instance (part estimée) | Dropper < 1 ms, Rush < 4 ms, Bedwars 4×4 < 6 ms | profilage spark par phase |
| Création d'instance (main thread) | < 500 ms cumulés, découpés en étapes ≤ 50 ms | timer interne `InstanceManager` |
| Téléportation file → instance | < 2 s | horodatage `queue.join` → `GamePlayerJoinEvent` |
| Allocation mémoire / joueur / s (hot path) | aucune dans `PlayerMoveEvent` hors changement de bloc | JFR |

Dimensionnement de départ (à confirmer par test de charge P11) pour 1 JVM 6 Go / 4 cœurs dédiés : 8 Dropper (12 j.) **ou** 6 Rush 4v4 **ou** 4 Bedwars 4×4 (16 j.).

## 2. Règles de code (vérifiées par `perf-reviewer`)

1. **Main thread sacré** : aucun I/O, aucun `join()`, aucun verrou contesté. I/O sur virtual threads (`NexusScheduler.io()`).
2. **Scope instance** : itérer `instance.players()`, jamais `Bukkit.getOnlinePlayers()`. Index `World → Instance` en `IdentityHashMap`.
3. **Événements chauds** : `PlayerMoveEvent` → `if (!e.hasChangedBlock()) return;` en première ligne ; `EntityDamageEvent` → filtrer l'instance d'abord ; pas de lambda capturante allouée par appel dans ces listeners.
4. **Tâches répétées** : fréquence minimale utile (générateurs : 1 tâche par instance à 1 tick qui gère tous les générateurs via compteurs, pas 1 tâche par générateur ; scoreboard : 20 ticks).
5. **Composants Adventure** : pré-construire les composants statiques, mettre en cache les messages i18n rendus par locale.
6. **Pas de streams** dans les chemins par tick ; boucles simples sur des tableaux/listes.
7. **Pas de `Location#clone()` / `new Vector` par tick** : réutiliser des objets mutables locaux à la tâche.
8. **Entités** : limiter les items au sol (fusion, plafond par générateur), retirer les entités à la fin, pas d'IA pour les PNJ.
9. **Blocs** : suivre les blocs posés par joueur dans un `LongOpenHashSet` de positions packées (règle « ne casser que les blocs posés ») au lieu de métadonnées/PDC par bloc.
10. **BDD** : un seul aller-retour par action utilisateur, batch pour les rapports de match, index pour toute nouvelle requête (expliquer avec `EXPLAIN`).
11. **Redis** : pipeline pour les écritures multiples, debounce des `instance.update`, pas de `KEYS` (utiliser des SET d'index).

## 3. Mondes d'instance

Le chargement de monde est le poste le plus coûteux. Stratégie (`WorldProvider`) :

1. **Cache local de templates** : `cache/templates/<mode>/<map>/<version>/` extrait une fois (checksum vérifié) au préchauffage.
2. **Copie rapide** vers `worlds/instances/<instanceId>/` (sur **tmpfs** recommandé en prod — sous Pterodactyl via le Mount `nexus-tmpfs`, cf. `17-deploiement-pterodactyl.md` §4) — copie de `region/` seulement + `level.dat` minimal ; maps petites (bornées) pour limiter le nombre de régions.
3. `WorldCreator` avec générateur **void**, `keepSpawnLoaded = false` / pas de spawn chunks, `autoSave = false`, gamerules posées avant l'arrivée des joueurs (cycle jour/nuit, météo, apparition de mobs, régénération naturelle selon combat, annonces d'avancements, propagation du feu…) — **toujours via les constantes `GameRule` de l'API**, les identifiants textuels des gamerules ayant été renommés dans les versions récentes.
4. **Préchargement asynchrone** des chunks de jeu (`World#getChunkAtAsync` sur la zone de la map) pendant `CREATING`.
5. **Pool d'instances pré-créées** : l'orchestrateur maintient `min_waiting_instances` → une instance est toujours prête avant l'arrivée des joueurs.
6. Destruction : `Bukkit.unloadWorld(world, false)` puis suppression du dossier **sur un virtual thread**.
7. Option à évaluer en spike S4 : format « slime » en mémoire (Advanced Slime Paper) si compatible 26.1 au moment du P0 → `SlimeWorldProvider` derrière la même interface. Décision dans `ADR-004`.

## 4. Configuration serveur (fichiers dans `infra/config/`)

`server.properties` (serveurs de jeu)
```
view-distance=6
simulation-distance=4
network-compression-threshold=-1      # compression gérée par Velocity
online-mode=false                     # Velocity modern forwarding
sync-chunk-writes=false
spawn-protection=0
allow-nether=false
generate-structures=false
max-players=200
```

`config/paper-global.yml` (extraits)
```yaml
proxies:
  velocity: { enabled: true, online-mode: true, secret: ${VELOCITY_SECRET} }
chunk-system:
  io-threads: 2
  worker-threads: 2
```

`config/paper-world-defaults.yml` (extraits)
```yaml
chunks:
  auto-save-interval: -1
  entity-per-chunk-save-limit: { experience_orb: 0, arrow: 8, snowball: 8 }
entities:
  spawning: { per-player-mob-spawns: true, monster-spawn-max-light-level: 0 }
  behavior: { disable-chest-cat-detection: true }
environment:
  optimize-explosions: true
  treasure-maps: { enabled: false }
misc:
  redstone-implementation: ALTERNATE_CURRENT
hopper: { disable-move-event: true }
tick-rates: { grass-spread: 400, container-update: 1 }
collisions: { max-entity-collisions: 2 }
```
> Vérifier chaque clé contre la doc Paper 26.1 au P0 (les clés évoluent entre versions) — tâche P0-05.

`spigot.yml` / `bukkit.yml` : `entity-activation-range` réduit, `merge-radius` item 3.5, `spawn-limits` à 0 (aucun mob naturel).

## 5. JVM (Java 25)

```
-Xms5G -Xmx5G
-XX:+UseCompactObjectHeaders          # Java 25 : en-têtes d'objets compacts, moins de mémoire
-XX:+UseZGC                            # option A : ZGC (générationnel uniquement depuis Java 24), pauses quasi nulles
# option B : flags G1 type « Aikar » adaptés Java 25
-XX:+AlwaysPreTouch -XX:+ParallelRefProcEnabled -XX:+PerfDisableSharedMem
-Dusing.aikars.flags=https://mcflags.emc.gs
```
Choix A/B tranché par le test de charge P11 (mesurer p99 MSPT, pas la moyenne). Ne pas ajouter `-XX:+ZGenerational` (obsolète depuis que ZGC est uniquement générationnel).

## 6. Observabilité

- **spark** installé sur tous les serveurs (profilage à la demande, `/spark tickmonitor`).
- Métriques Nexus exposées par l'orchestrateur (`/metrics` Prometheus) : joueurs, files, instances par état, MSPT par serveur, durée moyenne d'attente, échecs de création.
- Logs structurés côté orchestrateur (JSON).

## 7. Test de charge (P11)

- Bots : client headless capable de 26.1 (ex. outils de stress basés sur MCProtocolLib — **à vérifier** au moment du test) : 200 bots qui font la file, se déplacent, cassent/posent, tapent.
- Scénarios : (1) 12 Dropper simultanés, (2) 4 Bedwars 4×4 + 4 Rush, (3) rafale de 100 connexions en 10 s, (4) perte d'un serveur de jeu en pleine charge.
- Critère : NFR-01 respecté, aucune erreur dans les logs, aucune instance orpheline après 1 h.
