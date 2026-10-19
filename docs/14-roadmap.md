# 14 — Roadmap d'implémentation

Ordre pensé pour avoir **le plus tôt possible une boucle complète jouable** (Dropper de bout en bout en fin de P5),
puis empiler le combat, les jeux complexes et l'optimisation.
Chaque tâche : **ID**, objectif, docs à lire, critères d'acceptation (CA). Utiliser `/implement-task <ID>`.
Cocher `[x]` dans le même commit que l'implémentation.

---

## P0 — Fondations & spikes

- [x] **P0-01 — Monorepo Gradle**
  Docs : `02-project-structure.md`, `CLAUDE.md`.
  CA : `settings.gradle.kts` avec tous les modules (vides compilables) ; `build-logic` avec les 5 plugins de convention ; `libs.versions.toml` vérifié et figé sur les dernières versions stables (Paper 26.1.2, Velocity compatible 26.1, libs) ; toolchain Java 25 ; `./gradlew build` vert.
- [ ] **P0-02 — Règles de dépendance**
  CA : tâche `checkDependencyRules` qui échoue si un `games/*` dépend d'autre chose que `nexus-api` (test : ajouter une dépendance interdite → échec).
- [ ] **P0-03 — Shadow vs loader de bibliothèques Paper**
  CA : ADR-009 rédigé ; mécanisme retenu appliqué à `nexus-core` (Hikari, Flyway, Lettuce, Caffeine) ; plugin démarre sur Paper 26.1.2.
- [ ] **P0-04 — Environnement de dev Docker**
  Docs : `infra/README.md`.
  CA : `docker compose -f infra/docker-compose.dev.yml up` démarre Postgres, Valkey, Velocity, lobby-1, game-1, editor, orchestrator ; connexion d'un client 26.1 au proxy → arrivée sur lobby-1 ; `collectJars` dépose les jars aux bons endroits.
- [ ] **P0-05 — Configs Paper/Velocity de référence**
  Docs : `12-performance.md` §4.
  CA : fichiers dans `infra/config/` validés contre la doc Paper 26.1 (clés existantes), modern forwarding actif.
- [ ] **P0-06 — Spike S1 block-hit** → ADR-007. Docs : `10-pvp-legacy.md` §6.
- [ ] **P0-07 — Spike S2 application des dégâts** → ADR-008.
- [ ] **P0-08 — Spike S3 knockback** → noté dans `10-pvp-legacy.md`.
- [ ] **P0-09 — Spike S4 chargement de monde** (copie tmpfs vs slime) : mesure du temps main thread pour une map Bedwars 4 équipes → ADR-004 complété.
- [ ] **P0-10 — CI** : workflow build + `check` + integrationTest + artefacts ; la CI rejoue aussi le contrôle d'attribution du hook `commit-msg` sur tous les commits de la PR.
- [ ] **P0-11 — Outillage clean code**
  Docs : `15-clean-code.md` §2.
  CA : Spotless (palantir-java-format) + Error Prone + NullAway + Checkstyle (`config/checkstyle/nexus.xml` : méthode ≤ 40 lignes, fichier ≤ 400, ≤ 3 paramètres hors records, imbrication ≤ 3, nombres magiques) + JaCoCo (seuils) appliqués par `nexus.java` ; un test `ArchitectureTest` (ArchUnit) par module avec les règles de §2 ; `./gradlew check` les exécute tous ; un code volontairement fautif fait échouer chaque outil.
- [ ] **P0-12 — Hooks Git & settings**
  Docs : `16-git-workflow.md`.
  CA : tâche `installGitHooks` (copie `.githooks/*` dans `.git/hooks/`, exécutable, dépendance de `build`) ; le hook `commit-msg` rejette un message avec `Co-Authored-By`/lien de session/« Generated with » et accepte un message conforme ; `.claude/settings.json` vérifié (attribution vide) ; aucun commit de l'historique ne contient d'attribution (`git log --format=%B | grep -Ei 'co-authored-by|claude-session|generated with'` vide).

- [ ] **P0-13 — Livraison Pterodactyl**
  Docs : `17-deploiement-pterodactyl.md`, ADR-010.
  CA : tâche `packageRelease` (archives par rôle + SHA-256 + `nexus-boot.sh`) ; eggs importés dans un panel de test ; un serveur `game` et le proxy démarrent depuis une release publiée ; versions Java (`yolks:java_25`), Paper (Fill v3) et Velocity vérifiées et reportées dans la doc 17.

> Le tout premier commit du dépôt (import de cette documentation) se fait **avant** P0-01, par le développeur ou via `/commit`, avec le message `docs: spécifications et règles de développement`.

## P1 — Commun, données, socle Paper

- [ ] **P1-01 — `nexus-common`** : ids (records), `Result`, UUIDv7, JSON (Gson + adaptateurs), `PermissionResolver` + `PermissionTree` (résolution `08` §2, ≥ 90 % de couverture), `Permissions` (catalogue).
- [ ] **P1-02 — Protocole** : `Envelope`, tous les records de `04-messaging-protocol.md`, registre `type ↔ classe`, tests de sérialisation aller-retour.
- [ ] **P1-03 — `nexus-data` BDD** : `DataSourceFactory`, Flyway (migrate côté orchestrateur, validate ailleurs), repositories `Player`, `Role`, `Friend`, `GameMode`, `Config`, `Map`, `Match`, `Stats` ; tests Testcontainers.
- [ ] **P1-04 — `nexus-data` Redis** : `RedisConnector`, `RedisKeys`, `MessageBus` (publish, subscribe, request/reply avec timeout), `PresenceStore` ; tests Testcontainers (reconnexion incluse).
- [ ] **P1-05 — `nexus-core` bootstrap** : `PluginBootstrap` Paper, `ServiceRegistry`, lecture `nexus.yml` + env, `NexusScheduler` (virtual threads + `thenAcceptSync`), arrêt propre.
- [ ] **P1-06 — Profils joueurs** : chargement en `AsyncPlayerPreLoginEvent`, cache, sauvegarde du temps de jeu au quit, `player_name_history`.
- [ ] **P1-07 — i18n** : `MessageService`, fichiers FR/EN, locale joueur, `checkI18n`.
- [ ] **P1-08 — Permissions Paper** : `PermissionService`, attachments, développement des jokers, commandes `/role` & `/perm check`, invalidation Redis < 2 s (CA : test manuel 2 serveurs).
- [ ] **P1-09 — Framework UI** : `Menu` (pages, pagination, boutons, rafraîchissement), `DialogService` (wrappers sur l'API Dialog : formulaire, confirmation, notice), `ScoreboardService` (diff), `HologramService`.
- [ ] **P1-10 — Chat & tab list** : format préfixe/couleur du rôle d'affichage, tri tab list.

## P2 — Réseau : proxy & orchestrateur

- [ ] **P2-01 — `nexus-proxy` base** : connexion BDD/Redis, upsert joueur, présence (TTL, refresh 20 s), routage vers le lobby le moins chargé, enregistrement dynamique des serveurs depuis le registre (`registerServer`).
- [ ] **P2-02 — Permissions proxy** : `PermissionProvider` Velocity.
- [ ] **P2-03 — Heartbeats** : côté Paper (`server.heartbeat` 2 s, `capacityScore`, MSPT p95), `server.shutdown`, `server.drain`.
- [ ] **P2-04 — Orchestrateur base** : `Main`, config, Flyway migrate, élection de leader, `ServerRegistry` (DEAD > 10 s), métriques Prometheus, ligne de log exacte `Nexus orchestrator ready` une fois prêt (détection de l'egg).
- [ ] **P2-05 — Registre d'instances** : réception `instance.update/removed`, index `instances:<mode>:<variant>:waiting`.
- [ ] **P2-06 — Files & matchmaking** : `queue.join/leave/status`, `Matchmaker` (`05` §4), réservations, `player.transfer` ; tests unitaires avec faux serveurs (parties, places, FIFO, réservations expirées).
- [ ] **P2-07 — Réconciliation autonome** : boucle 500 ms, `min_waiting_instances`, `AllocationPolicy` (`05` §5), `instance.create` request/reply.
- [ ] **P2-08 — Commandes joueur proxy** : `/play <mode> [variante]`, `/leave`, `/lobby`, `/hub`.

## P3 — Runtime des jeux & API

- [ ] **P3-01 — `nexus-api` contrat** : toutes les interfaces de `06-game-api.md` (sans implémentation) + Javadoc + annotations de thread.
- [ ] **P3-02 — Registre des jeux** : `NexusApi.games().register`, vérifications au chargement (version API, mode en BDD, config par défaut valide).
- [ ] **P3-03 — Config typée** : lecture des annotations, `ConfigSchema`, (dé)sérialisation JSON ↔ record avec défauts, validation, `ConfigCache` invalidé par `config.updated`.
- [ ] **P3-04 — Maps** : `MapSchema` runtime (`MapView`, `bind`, validation), `MapCache`, coordonnées relatives → `Location`.
- [ ] **P3-05 — `WorldProvider`** : stratégie retenue au P0-09, préchauffage des templates (checksum), création/destruction asynchrones, pool.
- [ ] **P3-06 — `InstanceManager` & `EventDispatcher`** : création sur `instance.create`, index `World/UUID → Instance`, bus scopé, gestion d'erreurs (10/min → abort), `InstanceScheduler`.
- [ ] **P3-07 — Phases** : `PhaseMachine`, `WaitingPhase`, `CountdownPhase` (règles `05` §3), `EndPhase`, `PhaseContext` scopé.
- [ ] **P3-08 — Joueurs & équipes** : `GamePlayer`, `PlayerKey`, `TeamSpec`, `TeamAssigner` (parties ensemble), validation des réservations à la connexion, rejoin.
- [ ] **P3-09 — Pipeline de mort & modules de base** : `DeathPipelineModule`, `VoidDeathModule`, `RespawnModule`, `SpectatorModule`, `NoHunger`, `DamageRules`, `BuildRules`, `TeamChat`, `Kit`, `TimeLimit`, `Checkpoint`.
- [ ] **P3-10 — Stats & rapport de match** : `StatsRecorder`, `GameResult`, `MatchReporter` (transaction idempotente `03` §3), journal local en cas de panne BDD, `match.started/finished`.
- [ ] **P3-11 — UI de jeu** : sidebar par jeu (`ScoreboardSpec`), boss bars, titres, i18n broadcast.
- [ ] **P3-12 — `nexus-api-testkit`** : `FakeGameContext`, horloge, joueurs factices.
- [ ] **P3-13 — Commandes d'instance** : `/game create|start|stop|invite|kick|code|settings`, `/join <code>`, `instance.create-request`.

## P4 — Admin & éditeur

- [ ] **P4-01 — Rendu automatique des configs** : catégories → Dialogs, listes → menus, brouillon, récap diff, publication, historique, rollback, presets (`07` §2–4).
- [ ] **P4-02 — Hub admin** : réseau, parties en cours (observer/forcer/arrêter), créer une partie, modes/variantes (toggle), joueurs, journal.
- [ ] **P4-03 — Éditeur de maps** : profil `editor`, création/import, outils générés depuis le `MapSchema`, marqueurs visibles (Display), checklist, boss bar de progression.
- [ ] **P4-04 — Publication de maps** : validation par variante, nettoyage, archive `.tar.zst` + SHA-256, stockage templates, `map_versions`, `map.published`.
- [ ] **P4-05 — Audit** : toutes les actions admin dans `audit_log`.

## P5 — Dropper (premier jeu, valide toute la chaîne)

- [ ] **P5-01 — Module `game-dropper`** selon `docs/games/dropper.md` : config, schéma de map, phase Race, classement (logique pure testée), records.
- [ ] **P5-02 — Map de démonstration** (3 niveaux) construite dans l'éditeur et publiée.
- [ ] **P5-03 — Recette bout en bout** : file auto → partie → stats en BDD → retour lobby → « Rejouer ». Checklist de la spec cochée.

## P6 — Social

- [ ] **P6-01 — Amis** (`09` §1) : commandes, préférences, statut en ligne, `/ami rejoindre`.
- [ ] **P6-02 — Messages privés** (`09` §2).
- [ ] **P6-03 — Party** (`09` §3) : commandes, invitations, TTL, chat, warp, dissolution.
- [ ] **P6-04 — Party ↔ files** : chef seul, membres en partie bloquants, même équipe, « Rejouer » pour toute la party, invitations auto aux parties privées.
- [ ] **P6-05 — Chat staff** et chats cross-serveur.

## P7 — Combat legacy

- [ ] **P7-01 — `LegacyDamageCalculator` + `LegacyTables`** + tests de référence.
- [ ] **P7-02 — `KnockbackCalculator`** + profils + tests.
- [ ] **P7-03 — `LegacyCombatModule`** : C1–C18 selon les ADR-007/008, activation/désactivation par instance, profils éditables dans l'UI.
- [ ] **P7-04 — Recette** : checklist `10-pvp-legacy.md` §7 + test à 100 ms de latence.

## P8 — Rush

- [ ] **P8-01 — Modules partagés** : `BedModule`, `GeneratorModule` (+ hologrammes), `ShopModule` (catalogue éditable).
- [ ] **P8-02 — `game-rush`** selon `docs/games/rush.md`.
- [ ] **P8-03 — Map de démonstration 2 équipes** + recette.

## P9 — Bedwars

- [ ] **P9-01 — `game-bedwars`** : événements chronométrés, générateurs à paliers, boutique d'objets.
- [ ] **P9-02 — Améliorations d'équipe & pièges**.
- [ ] **P9-03 — Mort subite** (bordure), fin, cas limites de la spec.
- [ ] **P9-04 — Maps de démonstration** (8 équipes et 4 équipes) + recette.

## P10 — Lobby & classements

- [ ] **P10-01 — `nexus-lobby`** : spawn, hotbar (jeux, profil, amis, party, paramètres), protection, double saut/parcours (option).
- [ ] **P10-02 — Sélecteur de jeux** : menu + PNJ/`Interaction` par mode, compteurs temps réel.
- [ ] **P10-03 — Leaderboards** : job orchestrateur (ZSET `lb:*`, 60 s + debounce sur `match.finished`), menu (mode/variante/période/stat), hologrammes cliquables, records Dropper par map.
- [ ] **P10-04 — Profil & historique** : `/stats [joueur]`, `/historique`, menus.
- [ ] **P10-05 — Purge des périodes** (`03` §2 rétention) et expiration des rôles temporaires (job orchestrateur).

## P11 — Durcissement & scaling

- [ ] **P11-01 — Provider `pterodactyl`** (`05` §6, `17` §6) : mode pool (API Application pour lister, API Client pour start/stop, état `PENDING` jusqu'au premier heartbeat, drain avant arrêt) ; tests avec un faux panel HTTP ; mode `create` en option.
- [ ] **P11-02 — Pannes** : scénarios du tableau `01` §4 testés (kill d'un game, de Redis, de Postgres, failover orchestrateur).
- [ ] **P11-03 — Test de charge** (`12` §7) et réglage JVM/configs ; rapport dans `docs/perf-report.md`.
- [ ] **P11-04 — Audit `perf-reviewer`** sur tous les modules, corrections.
- [ ] **P11-05 — Exploitation** : dérouler la checklist `17-deploiement-pterodactyl.md` §7 et §8 sur l'environnement réel, corriger la doc 17 selon le constat, restauration d'une sauvegarde Postgres testée.

---

## Jalons

| Jalon | Fin de | Démonstration |
|---|---|---|
| **M1 — Ça tourne** | P2 | Connexion au proxy, lobby, `/play` met en file (sans jeu réel) |
| **M2 — Première partie** | P5 | Dropper jouable en autonome, stats en BDD |
| **M3 — Réseau social** | P6 | Parties entre amis en party |
| **M4 — PvP** | P8 | Rush jouable en 1.8 |
| **M5 — Complet** | P10 | Bedwars, lobby, classements |
| **M6 — Prod** | P11 | Tenue en charge, scaling, pannes maîtrisées |
