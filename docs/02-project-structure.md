# 02 — Structure du projet

## 1. Modules Gradle

```
settings.gradle.kts
includeBuild("build-logic")

include(
  ":common:nexus-common",
  ":common:nexus-data",
  ":platform:nexus-api",
  ":platform:nexus-api-testkit",
  ":platform:nexus-combat",
  ":platform:nexus-core",
  ":platform:nexus-lobby",
  ":proxy:nexus-proxy",
  ":services:nexus-orchestrator",
  ":games:game-dropper",
  ":games:game-rush",
  ":games:game-bedwars",
)
```

| Module | Type | Dépend de | Artefact |
|---|---|---|---|
| `nexus-common` | lib Java pur | — (Gson compileOnly, JSpecify) | jar |
| `nexus-data` | lib Java pur | `nexus-common`, HikariCP, Flyway, Postgres JDBC, Lettuce, Caffeine | jar (shadé dans les consommateurs, **relocalisé**) |
| `nexus-api` | lib Paper | `nexus-common`, `paper-api` (compileOnly) | jar publié (Maven local) pour les jeux |
| `nexus-api-testkit` | lib test | `nexus-api` | `FakeGameContext`, horloge contrôlable, joueurs factices |
| `nexus-combat` | lib Paper | `nexus-api`, `paper-api` (compileOnly) | jar (shadé dans core) |
| `nexus-core` | plugin Paper | `nexus-api`, `nexus-combat`, `nexus-data`, `nexus-common` | `NexusCore-<v>.jar` |
| `nexus-lobby` | plugin Paper | `nexus-api` + API de service du core exposée via `ServicesManager` (`LobbyBridge`) | `NexusLobby-<v>.jar` |
| `nexus-proxy` | plugin Velocity | `nexus-data`, `nexus-common`, `velocity-api` (compileOnly) | `NexusProxy-<v>.jar` |
| `nexus-orchestrator` | application | `nexus-data`, `nexus-common`, docker-java (provider docker) | `nexus-orchestrator-<v>.jar` + image Docker |
| `game-*` | plugin Paper | `nexus-api` (compileOnly) **uniquement** | `NexusGame-<Id>-<v>.jar` |

### Relocalisation (obligatoire)
Les jars shadés relocalisent leurs dépendances tierces sous `fr.nexus.libs.*` (Hikari, Flyway, Lettuce, Netty de Lettuce, Caffeine)
pour éviter les conflits avec d'autres plugins. Exception : rien n'est relocalisé dans `nexus-orchestrator` (application autonome).

> Alternative à évaluer en P0 : le **loader de bibliothèques** de `paper-plugin.yml` (`PluginLoader` + `MavenLibraryResolver`) pour ne pas shader. Décision à consigner dans un ADR.

## 2. `build-logic` (plugins de convention)

| Plugin | Applique |
|---|---|
| `nexus.java` | toolchain Java 25, `-parameters`, `-Xlint:all -Werror` (hors dépréciations Paper), encodage UTF-8, JUnit 5, AssertJ, JSpecify |
| `nexus.paper-plugin` | `nexus.java` + `paper-api` compileOnly + génération/validation `paper-plugin.yml` + `run-paper` pour test local |
| `nexus.game` | `nexus.paper-plugin` + `nexus-api` compileOnly + tâche `verifyGameDependencies` (échoue si une autre dépendance projet est ajoutée) |
| `nexus.shadow` | Shadow + relocalisations standard + `minimize()` contrôlé |
| `nexus.integration-test` | source set `integrationTest` + Testcontainers |

La tâche racine `checkDependencyRules` agrège les vérifications :
- `games/*` → seulement `:platform:nexus-api` ;
- `nexus-common` → aucune dépendance projet ;
- `nexus-data` → seulement `nexus-common` ;
- aucun module autre que `nexus-core` ne dépend de `nexus-combat`.

## 3. Packages

```
fr.nexus.common
  ├─ model        (PlayerId, ServerId, InstanceId, ModeId, VariantId, MatchId : records value-object)
  ├─ protocol     (Envelope, MessageType, messages par domaine : server/, instance/, queue/, social/, perm/)
  ├─ json         (Gson configuré : adaptateurs Instant, UUID, records)
  └─ util         (Durations, Ids (UUIDv7), Result<T,E>)

fr.nexus.data
  ├─ db           (DataSourceFactory, Migrations, Tx (helper transaction))
  ├─ repo         (PlayerRepository, RoleRepository, StatsRepository, MatchRepository, ConfigRepository, MapRepository, FriendRepository)
  ├─ repo.pg      (implémentations Postgres)
  ├─ redis        (RedisConnector, RedisKeys, MessageBus, PresenceStore, LeaderboardCache)
  └─ cache        (Caches Caffeine partagés)

fr.nexus.api
  ├─ NexusApi     (point d'accès statique minimal : games(), players(), i18n(), scheduler())
  ├─ game         (GameDefinition, GameContext, GameInstance, GameResult, GameState, Variant)
  ├─ phase        (Phase, PhaseContext, phases fournies)
  ├─ team         (TeamSpec, Team, TeamAssigner)
  ├─ module       (GameModule + modules fournis)
  ├─ config       (GameConfig, @Setting, @Category, @Range, ConfigSchema)
  ├─ map          (MapData, MapSchema, Marker, MarkerType, Region)
  ├─ stats        (StatKey, StatAggregation, RecordKey)
  ├─ player       (GamePlayer, PlayerState, Kit)
  ├─ ui           (ScoreboardSpec, Menu, DialogForm, Hologram)
  ├─ event        (événements Nexus : GamePlayerJoinEvent, PlayerEliminatedEvent, PhaseChangeEvent, ...)
  ├─ combat       (CombatProfile, KnockbackProfile — interfaces)
  └─ annotation   (@MainThread, @AnyThread)

fr.nexus.core
  ├─ bootstrap    (NexusCorePlugin, NexusBootstrap (PluginBootstrap Paper), ServiceRegistry)
  ├─ scheduler    (NexusScheduler : io(), sync(), later(), timer())
  ├─ player       (ProfileService, ProfileCache, listeners prelogin/join/quit)
  ├─ permission   (PermissionService, PermissibleInjector, RoleCache)
  ├─ i18n         (MessageService, LocaleResolver)
  ├─ instance     (InstanceManager, DefaultGameInstance, EventDispatcher, WorldProvider, InstanceRegistryPublisher)
  ├─ phase        (PhaseMachine)
  ├─ stats        (MatchReporter, PendingReportJournal)
  ├─ ui           (MenuFramework, DialogService, ScoreboardService, HologramService)
  ├─ admin        (AdminHub, ConfigEditor (rendu auto des schémas), InstanceMonitor)
  ├─ editor       (MapEditorService, MarkerTools, MapPublisher)   ← actif seulement en profil editor
  ├─ command      (commandes Brigadier)
  └─ messaging    (abonnements Redis côté serveur)

fr.nexus.combat   (LegacyCombatModule, CombatCalculator (pur), KnockbackCalculator (pur), LegacyItems, listeners)
fr.nexus.lobby    (LobbyPlugin, GameSelector, LeaderboardHolograms, Npcs, HotbarItems)
fr.nexus.proxy    (NexusProxyPlugin, Routing, QueueBridge, PartyService, FriendService, MessagingCommands, ProxyPermissionProvider)
fr.nexus.orchestrator (Main, ServerRegistry, InstanceRegistry, QueueService, Matchmaker, AllocationPolicy, ScalingProvider (static/docker), LeaderboardJob, LeaderElection)
fr.nexus.game.<id> (un package par jeu)
```

## 4. Fichiers de config

| Fichier | Où | Contenu |
|---|---|---|
| `nexus.yml` | chaque serveur Paper (`plugins/NexusCore/`) | id serveur (ou `NEXUS_SERVER_ID`), type, connexions BDD/Redis (variables d'env prioritaires), capacité max d'instances, langue par défaut |
| `nexus-proxy.yml` | Velocity | id proxy, BDD/Redis, nom des lobbys par défaut, messages de maintenance |
| `orchestrator.yml` | orchestrateur | BDD/Redis, provider de scaling, politiques par mode (min instances, priorité) |
| `lang/*.yml` | ressources de chaque plugin | messages MiniMessage ; surcharge possible dans `plugins/<Plugin>/lang/` |

Les **réglages de jeu** ne sont **pas** dans des fichiers : ils sont en BDD (`game_configs`) et édités par l'UI.

Toutes les clés acceptent une variable d'environnement (`NEXUS_DB_URL`, `NEXUS_DB_USER`, `NEXUS_DB_PASSWORD`, `NEXUS_REDIS_URL`, `NEXUS_SERVER_ID`…) : c'est ainsi que les eggs Pterodactyl configurent les serveurs. Exemples complets : `infra/config/nexus/*.example`.

## 5. Versionnement & livraison

- SemVer commun à tout le monorepo (`gradle.properties` → `version`), `nexusApiVersion` séparée.
- CI (GitHub Actions / Gitea Actions) : `build`, `test`, `integrationTest`, `checkDependencyRules`, artefacts jars, image Docker de l'orchestrateur.
- `collectJars` organise `build/dist/{proxy,lobby,game,editor,orchestrator}/` (dev, docker-compose).
- `packageRelease` produit `build/release/nexus-<rôle>.tar.gz` + `.sha256` + `nexus-boot.sh`, consommés par les serveurs Pterodactyl (`17-deploiement-pterodactyl.md` §5).
