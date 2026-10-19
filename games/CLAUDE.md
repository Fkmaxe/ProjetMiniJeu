# CLAUDE.md — modules de jeux (`games/*`)

Tu travailles sur un mini-jeu. Règles spécifiques en plus du `CLAUDE.md` racine :

1. **Seule dépendance autorisée : `nexus-api`** (scope `compileOnly`). Pas de `nexus-core`, pas d'autre jeu, pas de `nexus-data`.
2. Point d'entrée unique : une classe qui implémente `GameDefinition<C, M>` et qui est enregistrée dans le `GameBootstrap` du plugin (`NexusApi.games().register(...)`).
3. Le jeu ne gère **ni** la file d'attente, **ni** le chargement du monde, **ni** l'écriture des stats en BDD, **ni** le scoreboard de base, **ni** la téléportation de fin : c'est le runtime. Le jeu **déclare** (phases, équipes, modules, stats) et **réagit** (événements scopés).
4. Toute règle chiffrée vient de la **config typée** (`C extends GameConfig`) — jamais de nombre magique dans la logique. Chaque champ : `@Setting(label, description, category)` + bornes + défaut.
5. Toute position vient de la **map** (`M extends MapData`), déclarée par un `MapSchema` (marqueurs que l'éditeur de map fait placer). Jamais de coordonnées en dur.
6. Réutiliser les **modules** de `nexus-api` avant d'écrire du code (`VoidDeath`, `Respawn`, `TeamChat`, `Generators`, `Shop`, `BedDefense`, `Spectator`, `LegacyCombat`, `NoHunger`, `BuildLimits`…). Si un comportement sert à 2 jeux, il devient un module de l'API.
7. Les stats sont déclarées (`StatKey`) et incrémentées via `ctx.stats()`. Victoire/défaite/participation sont **automatiques** à partir du résultat de partie (`ctx.end(GameResult)`).
8. Tests : la logique (économie, scoring, conditions de victoire) dans des classes pures testées avec JUnit ; le reste via le harness `nexus-api-testkit` (`FakeGameContext`).

Structure attendue d'un module :

```
games/game-<id>/
├─ build.gradle.kts            (plugin de convention `nexus.game`)
├─ src/main/java/fr/nexus/game/<id>/
│  ├─ <Id>Bootstrap.java       (JavaPlugin minimal → register)
│  ├─ <Id>Definition.java      (GameDefinition)
│  ├─ <Id>Config.java          (record de config annoté)
│  ├─ <Id>Map.java + <Id>MapSchema.java
│  ├─ phase/                   (phases spécifiques)
│  ├─ logic/                   (Java pur, testable)
│  └─ ui/                      (scoreboard lines, menus spécifiques)
├─ src/main/resources/
│  ├─ paper-plugin.yml
│  └─ lang/fr_FR.yml, en_US.yml
└─ src/test/java/...
```

Spécifications : `docs/games/<id>.md`. API : `docs/06-game-api.md`.
