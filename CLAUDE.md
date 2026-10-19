# CLAUDE.md — Nexus (réseau mini-jeux Minecraft 26.1)

> Ce fichier est lu automatiquement par Claude Code à chaque session. Il donne le contexte, les règles
> non négociables et l'index de la documentation. **Lis le document `docs/` pertinent avant de coder
> une fonctionnalité** — les specs y sont détaillées, ce fichier n'est qu'un point d'entrée.

## 1. Le projet en 5 lignes

Nexus est un ensemble de plugins Minecraft **Paper 26.1.x** + **Velocity** interconnectés par
**PostgreSQL** et **Redis (Valkey)**, formant un serveur mini-jeux :
- les parties se lancent **toutes seules** (file d'attente → compte à rebours → partie) **ou à la demande** (admin / chef de party) ;
- l'infrastructure est **répartie sur plusieurs serveurs** (proxy, lobbys, serveurs de jeu multi-arènes, orchestrateur) ;
- ajouter un mini-jeu = **un module Gradle qui ne dépend que de `nexus-api`** (cf. `docs/06-game-api.md`) ;
- chaque jeu expose une **config typée** rendue automatiquement en **interface admin in-game** (Dialogs + menus) ;
- jeux de départ : **Dropper**, **Rush** (façon Funcraft), **Bedwars** (classique), avec **PvP 1.8** quand nécessaire et les **animations modernes** (Display entities, Dialogs, etc.).

## 2. Stack (versions cibles — vérifier dans `gradle/libs.versions.toml`)

| Élément | Choix | Notes |
|---|---|---|
| Langage | **Java 25** (LTS) | 26.1 exige Java 25. Records, sealed, pattern matching, virtual threads autorisés. |
| Serveur | **Paper 26.1.2** (API `io.papermc.paper:paper-api`) | 26.1 est non obfusqué. **Aucun NMS** (cf. ADR-006). |
| Proxy | **Velocity** (dernière stable compatible 26.1) | Modern forwarding obligatoire. |
| Orchestrateur | Service Java autonome (`services/nexus-orchestrator`) | Pas un plugin. |
| BDD | **PostgreSQL 17** + **Flyway** + **HikariCP** | Accès via repositories JDBC (pas d'ORM). |
| Cache / bus | **Valkey 8** (compatible Redis) via **Lettuce** | Pub/Sub + clés à TTL + ZSET leaderboards. |
| Cache local | **Caffeine** | |
| Sérialisation | **Gson** (fourni par Paper) côté plugins, idem services | Un seul format JSON partout. |
| Texte | **Adventure + MiniMessage** | Tous les messages via i18n, jamais de `§`. |
| Commandes | **Brigadier via l'API Paper** (`LifecycleEvents.COMMANDS`) | Pas de `plugin.yml commands:`. |
| Build | **Gradle (Kotlin DSL)**, `build-logic/` conventions, **Shadow** pour les jars | |
| Tests | JUnit 5, AssertJ, **Testcontainers** (Postgres/Valkey) | Logique de jeu testable sans serveur. |
| Dev local | `infra/docker-compose.dev.yml` (Postgres, Valkey, Velocity, lobby, game, orchestrator) | Dev uniquement. |
| **Déploiement** | **Pterodactyl** (Panel + Wings) : 3 eggs + `nexus-boot.sh` dans `infra/pterodactyl/`, Postgres/Valkey hors panel (`infra/prod/`) | Référence : `docs/17-deploiement-pterodactyl.md` (ADR-010). Scaling = pool de serveurs démarrés/arrêtés par l'orchestrateur via l'API du panel. |

## 3. Carte du dépôt

```
nexus/
├─ CLAUDE.md                ← ce fichier
├─ .claude/                 ← settings (attribution désactivée, permissions), commandes (/implement-task, /commit, /new-minigame, /perf-check), agents (clean-code-reviewer, perf-reviewer, api-boundary-reviewer)
├─ .github/                 ← CI (workflows/ci.yml) et modèle de PR
├─ config/checkstyle/       ← règles Checkstyle (docs/15-clean-code.md)
├─ .githooks/               ← commit-msg (format + refus d'attribution), pre-commit (format, fichiers interdits) — installés par ./gradlew installGitHooks
├─ docs/                    ← TOUTES les specs (index §7)
├─ gradle/libs.versions.toml
├─ build-logic/             ← plugins de convention Gradle (java, paper-plugin, shadow, tests)
├─ common/
│  ├─ nexus-common/         ← Java pur : modèles, DTO, protocole Redis, utilitaires. Zéro dépendance MC.
│  └─ nexus-data/           ← Java pur : Postgres (Flyway, repos), Redis (Lettuce), caches. Zéro dépendance MC. (CLAUDE.md dédié, migrations V1/V2 déjà écrites)
├─ platform/
│  ├─ nexus-api/            ← API PUBLIQUE pour les jeux (interfaces Paper). Stable, versionnée. (CLAUDE.md dédié)
│  ├─ nexus-api-testkit/    ← FakeGameContext & outils de test des jeux sans serveur.
│  ├─ nexus-core/           ← plugin Paper : runtime des instances, joueurs, perms, GUI, admin UI, stats.
│  ├─ nexus-combat/         ← librairie : PvP legacy 1.8 (calculs purs + listeners), shadée dans core.
│  └─ nexus-lobby/          ← plugin Paper : lobby, sélecteurs, PNJ, hologrammes de leaderboards.
├─ proxy/nexus-proxy/       ← plugin Velocity : routage, files, party, amis, perms proxy, messages.
├─ services/nexus-orchestrator/ ← service autonome : registre serveurs, matchmaking, allocation d'instances.
├─ games/                   ← (CLAUDE.md dédié : règles des modules de jeu)
│  ├─ game-dropper/
│  ├─ game-rush/
│  └─ game-bedwars/
└─ infra/
   ├─ docker-compose.dev.yml  ← réseau complet en local (dev)
   ├─ config/                 ← configs de référence Paper/Velocity/Nexus
   ├─ pterodactyl/            ← eggs + nexus-boot.sh (PRODUCTION)
   └─ prod/                   ← Postgres/Valkey/sauvegardes de production (hors panel)
```

Règles de dépendance (vérifiées par le build, cf. `docs/02-project-structure.md`) :
`games/* → nexus-api` **uniquement** · `nexus-core → nexus-api, nexus-combat, nexus-data, nexus-common` ·
`nexus-proxy, nexus-orchestrator → nexus-data, nexus-common` · `nexus-common` ne dépend de rien.

## 4. Commandes

```bash
./gradlew installGitHooks            # installe les hooks Git du dépôt (fait aussi automatiquement par build)
./gradlew check                      # TOUT : format, Error Prone/NullAway, Checkstyle, ArchUnit, tests, couverture, i18n
./gradlew spotlessApply              # reformate le code
./gradlew build                      # compile + tests + shadowJar de tous les modules
./gradlew :games:game-bedwars:build  # un seul module
./gradlew test                       # tests unitaires
./gradlew integrationTest            # tests Testcontainers (Docker requis)
./gradlew checkDependencyRules       # vérifie les règles de dépendance entre modules
./gradlew collectJars                # copie les jars dans build/dist/{proxy,lobby,game,editor,orchestrator}/
docker compose -f infra/docker-compose.dev.yml up -d   # réseau complet local
docker compose -f infra/docker-compose.dev.yml logs -f game-1
```

## 5. Règles NON négociables

### Threading (la cause n°1 de lag)
1. **Jamais d'I/O sur le main thread** (BDD, Redis, fichiers, HTTP). Utiliser `NexusScheduler.io()` (executor de virtual threads) puis revenir sur le main thread avec `NexusScheduler.sync()` / le scheduler de l'entité.
2. **Jamais d'appel à l'API Bukkit hors main thread**, sauf méthodes documentées thread-safe (envoi de messages Adventure, `AsyncPlayerPreLoginEvent`).
3. Les données joueur sont chargées dans `AsyncPlayerPreLoginEvent` (bloquant autorisé, déjà async) et mises en cache — **pas** au `PlayerJoinEvent`.
4. Pas de `Thread.sleep`, pas de `CompletableFuture#join()` sur le main thread.

### Architecture
5. Un module de jeu **n'importe jamais** `fr.nexus.core.*`, `fr.nexus.data.*` ni de classe d'un autre jeu. Tout passe par `nexus-api`. Si un besoin manque dans l'API, **on l'ajoute à l'API** (avec doc), on ne contourne pas.
6. **Aucun NMS / CraftBukkit / réflexion sur les internals.** Paper API + Adventure uniquement. Si une fonctionnalité semble impossible, écrire un spike et le documenter dans `docs/adr/`.
7. Un serveur de jeu héberge **plusieurs instances** ; chaque instance a son monde. **Tout état de jeu est scopé à l'instance** (jamais de `static` mutable, jamais de `Bukkit.getOnlinePlayers()` dans la logique de jeu → `instance.players()`).
8. Les listeners de jeu passent par le **bus d'événements scopé** de l'instance (`GameContext#events()`), jamais `Bukkit.getPluginManager().registerEvents` direct dans un jeu.
9. La logique de règles (calcul de dégâts, scores, économie, timers) est écrite en **Java pur testable** ; les classes Bukkit ne sont que des adaptateurs.

### Données
10. Toute modification de schéma = **nouvelle migration Flyway** `V<n>__<description>.sql`. On ne modifie jamais une migration déjà mergée.
11. Les stats de fin de partie s'écrivent **en une transaction idempotente** (clé = `match_id`).
12. Clés Redis et messages : **uniquement** ceux définis dans `docs/04-messaging-protocol.md`. Nouveau message = mise à jour du doc + record dans `nexus-common/protocol`.

### Qualité
13. Tous les textes joueurs passent par l'i18n (`lang/fr_FR.yml`, `lang/en_US.yml`) en MiniMessage. Langue par défaut : **français**.
14. Toute permission est déclarée dans le catalogue (`docs/08-permissions-roles.md` + `Permissions.java`).
15. Toute option de config de jeu a un **libellé, une description, des bornes et une valeur par défaut** (sinon l'UI admin ne peut pas la rendre).
16. Pas de dépendance nouvelle sans l'ajouter au version catalog et la justifier dans la PR/commit.
17. **Clean code** (`docs/15-clean-code.md`) : noms métier du glossaire, fonctions ≤ 40 lignes / ≤ 3 paramètres / imbrication ≤ 3, une responsabilité par classe, aucune règle de jeu dans un listener/commande/menu, aucun nombre ni chaîne magique, pas de code commenté, `TODO(<ID>)` uniquement. Imposé par Spotless, Error Prone + NullAway, Checkstyle, ArchUnit, JaCoCo via `./gradlew check`.

### Git (`docs/16-git-workflow.md`) — impératif
18. **Committer à chaque étape** cohérente et verte (pas seulement en fin de tâche) : une intention par commit, Conventional Commits en français, pied `Refs: <ID>`. Utiliser `/commit`.
19. Jamais de commit rouge : `./gradlew build` (ou `:<module>:check`) avant chaque commit, `./gradlew check` complet avant le commit de fin de tâche.
20. **Identité** : les commits utilisent l'identité Git déjà configurée par le développeur. Ne **jamais** exécuter `git config`, ni `--author`, ni `-c user.*`, ni `GIT_AUTHOR_*`/`GIT_COMMITTER_*`. Si `git config user.email` est vide → s'arrêter et demander.
21. **Aucune attribution** : pas de `Co-Authored-By`, pas de lien de session, pas de « Generated with … », aucune mention d'outil d'assistance dans les messages de commit, noms de branches ou descriptions de PR. (`.claude/settings.json` désactive l'attribution ; le hook `commit-msg` la rejette.)
22. Jamais `--no-verify`, jamais `git add -A` aveugle, jamais `push --force` sur une branche partagée.

### Déploiement (`docs/17-deploiement-pterodactyl.md`)
23. Tout changement qui touche le déploiement — nouvelle variable d'environnement ou clé de config, port, plugin/jar, montage, service, version Java/Paper/Velocity, contenu des archives de release, `nexus-boot.sh`, eggs — **met à jour `docs/17-deploiement-pterodactyl.md` (dont l'historique §11) et `infra/pterodactyl/` dans le même commit**. Un plan qui touche ces éléments doit le prévoir.
24. Aucun secret dans le dépôt : les secrets passent par les variables masquées des eggs ou `infra/prod/secrets/` (ignoré par Git).

## 6. Conventions de code (résumé — détail dans `docs/13-coding-standards.md`)

- Package racine `fr.nexus` (`fr.nexus.api`, `fr.nexus.core`, `fr.nexus.game.bedwars`, …).
- Records pour DTO/valeurs, interfaces pour services, implémentations `Default*`/`Redis*`/`Pg*`.
- Injection manuelle par constructeur (pas de framework DI). Les services sont créés dans le `Bootstrap` de chaque plugin.
- `@Nullable`/`@NonNull` JSpecify ; `Optional` en retour seulement.
- Logs via le logger du plugin (`ComponentLogger`/SLF4J), niveaux : `debug` verbeux, `info` cycle de vie, `warn` récupérable, `error` avec exception.
- Nommage des tests : `methodName_condition_expectedResult`.

## 7. Index de la documentation

| Doc | Quand le lire |
|---|---|
| `docs/00-vision-prd.md` | Exigences fonctionnelles (FR-xx) et non fonctionnelles (NFR-xx), hypothèses, questions ouvertes |
| `docs/01-architecture.md` | Vue d'ensemble, composants, flux (connexion, file, lancement, fin de partie) |
| `docs/02-project-structure.md` | Modules Gradle, packages, règles de dépendance, build-logic |
| `docs/03-database.md` | Schéma Postgres, clés Redis, stratégie de cache |
| `docs/04-messaging-protocol.md` | Canaux Redis, enveloppe, catalogue des messages |
| `docs/05-orchestration-matchmaking.md` | Cycle de vie serveurs/instances, mode autonome & à la demande, files, allocation |
| `docs/06-game-api.md` | **API pour créer un mini-jeu** + exemple complet |
| `docs/07-admin-config-ui.md` | Interfaces admin, config auto-générée, éditeur de maps |
| `docs/08-permissions-roles.md` | Rôles, permissions, catalogue des nœuds |
| `docs/09-social-party-friends.md` | Amis, party, messages privés |
| `docs/10-pvp-legacy.md` | Spécification du combat 1.8 sur 26.1 |
| `docs/11-visuals-animations.md` | Catalogue des effets modernes et règles de coût |
| `docs/12-performance.md` | Budgets, règles d'optimisation, configs Paper/JVM, tests de charge |
| `docs/13-coding-standards.md` | Conventions, threading, erreurs, tests |
| `docs/14-roadmap.md` | **Phases et tâches numérotées** à implémenter dans l'ordre |
| `docs/15-clean-code.md` | Principes de conception, outils de qualité, checklist de relecture |
| `docs/16-git-workflow.md` | Commits à chaque étape, format des messages, identité, branches, hooks |
| `docs/17-deploiement-pterodactyl.md` | **Déploiement production** (eggs, variables, montages, release, scaling, mise à jour) — à tenir à jour |
| `docs/games/*.md` | Spécifications de chaque mini-jeu |
| `docs/adr/*.md` | Décisions d'architecture et leur justification |
| `docs/glossary.md` | Vocabulaire (instance, variante, profil serveur…) |

## 8. Comment travailler sur ce dépôt (workflow Claude Code)

Le développeur travaille **en mode plan** : `/plan <demande>` (ou Shift+Tab → « plan mode »). En mode plan, Claude
explore et lit sans rien modifier, rédige le plan au format ci-dessous, puis attend l'approbation. Après approbation,
Claude exécute le plan **étape par étape, un commit par étape**. Si le plan doit changer en cours de route
(découverte, blocage), Claude s'arrête, explique et propose un plan révisé plutôt que d'improviser.

1. Tâche : la **prochaine non cochée** de `docs/14-roadmap.md`, celle demandée, ou `/implement-task <ID>` (compatible mode plan).
2. **Phase plan** (mode plan) : lire la tâche, les docs référencées, le `CLAUDE.md` du module, `15-clean-code.md`, `16-git-workflow.md` ; explorer le code existant ; produire le plan.
3. **Exécution** (après approbation) : `git switch -c feat/<ID>-<slug>` ; pour chaque étape du plan : implémenter → tests → `./gradlew :<module>:check` vert → `/commit <ID>`.
4. Avant le dernier commit : `./gradlew check` complet + agent `clean-code-reviewer` (+ `perf-reviewer` si code serveur, `api-boundary-reviewer` si jeu ou API).
5. Mettre à jour la doc si l'implémentation diffère de la spec (la doc est la source de vérité) et la doc 17 si le déploiement est touché.
6. Dernier commit : cocher la tâche dans la roadmap. Résumé : commits créés, écarts avec le plan.

### Format obligatoire d'un plan

```markdown
# Plan — <ID> <titre>
## Objectif            1-3 phrases + critères d'acceptation repris de la roadmap
## Docs lues           liste (avec §)
## Constat             ce qui existe déjà dans le code, ce qui manque
## Décisions           choix techniques + alternatives écartées ; questions bloquantes éventuelles
## Contrats impactés   nexus-api / schéma BDD (migration V<n>) / protocole Redis / permissions / i18n / config de jeu — « aucun » sinon
## Étapes = commits    1. <type(scope): résumé prévu> — fichiers — tests — commande de vérification
                       2. ...
## Tests               unitaires, intégration, recette manuelle
## Déploiement         impact sur docs/17 et infra/pterodactyl (variables, ports, archives) — « aucun » sinon
## Risques             perf / threading / rétrocompatibilité, et parade
```

Un plan sans section « Étapes = commits » ou sans « Déploiement » est incomplet.

### Definition of Done
- [ ] `./gradlew check` vert (format, analyse statique, Checkstyle, ArchUnit, tests, couverture, i18n, `checkDependencyRules`)
- [ ] Relecture `clean-code-reviewer` : « prêt à committer »
- [ ] Commits par étape, au format, avec `Refs:`, sans attribution, avec l'identité du développeur
- [ ] Le travail correspond au plan approuvé (écarts expliqués)
- [ ] Doc 17 et `infra/pterodactyl/` à jour si le déploiement est touché
- [ ] Aucun I/O sur main thread (relire chaque `CompletableFuture`/appel repo)
- [ ] Messages i18n FR + EN, permissions déclarées
- [ ] Options de config annotées (libellé, bornes, défaut) si jeu
- [ ] Doc mise à jour si nécessaire

## 9. Pièges connus

- Les versions Paper suivent le format `26.1.2.build.<n>-stable` : ne pas parser la version serveur à la main.
- `Bukkit.createWorld` est synchrone et coûteux : passer par `WorldProvider` (pré-chargement, pool) — cf. `docs/12-performance.md`.
- Les Display entities sont de vraies entités : les créer avec `setPersistent(false)` et les retirer à la destruction de l'instance.
- Un joueur peut se déconnecter **à n'importe quelle phase** : toute logique de phase doit gérer `onQuit`.
- Le proxy est la seule source de vérité de « sur quel serveur est le joueur » ; l'orchestrateur, celle de « dans quelle instance ».
- 26.2 est sortie : on reste sur 26.1.x tant que la roadmap ne dit pas le contraire, mais on n'utilise rien de déprécié.
