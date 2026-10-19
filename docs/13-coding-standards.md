# 13 — Standards de code & tests

## 1. Java

- Java 25, `--release 25`. Autorisé et encouragé : records, sealed interfaces, pattern matching (`switch` sur sealed), `var` local quand le type est évident, text blocks, virtual threads.
- Interdit : `Optional` en paramètre ou champ, `null` pour une collection (renvoyer vide), exceptions vides `catch (Exception e) {}`, `System.out`, état `static` mutable, `synchronized` sur le main thread pour des opérations longues.
- Nullabilité : packages annotés `@NullMarked` (JSpecify), `@Nullable` explicite.
- Immutabilité par défaut : `List.copyOf`, records ; les collections mutables restent privées.
- Une classe publique par fichier, < 400 lignes visées ; extraire la logique pure.
- Nommage : `XxxService` (interface métier), `DefaultXxxService`, `PgXxxRepository`, `RedisXxxStore`, `XxxListener`, `XxxCommand`, `XxxMenu`, `XxxDialog`, `XxxPhase`, `XxxModule`.

## 2. Threading

```java
// Pattern standard : I/O puis retour main thread
scheduler.io(() -> statsRepository.loadSummary(playerId))       // virtual thread
         .thenAcceptSync(summary -> menu.open(player, summary))   // repasse sur le main thread (ou le scheduler de l'entité)
         .exceptionally(err -> { log.error("stats load failed for {}", playerId, err); i18n.send(player, "error.generic"); return null; });
```

- `NexusScheduler` expose `io()`, `sync()`, `later(ticks)`, `timer(ticks)` et un `NexusFuture<T>` avec `thenAcceptSync` / `thenApplySync`.
- Une méthode qui bloque porte le suffixe implicite du package `repo` (bloquant par contrat) ou est annotée `@Blocking`.
- Annotations de thread `@MainThread` / `@AnyThread` sur les API publiques.

## 3. Erreurs

- Erreurs **attendues** (joueur introuvable, file refusée) → `Result<T, E>` avec `E` enum ou sealed ; affichage i18n.
- Erreurs **inattendues** → exception, log `error` avec contexte (joueur, instance, match), message générique au joueur.
- Un listener ne doit **jamais** laisser remonter une exception qui casserait l'instance : le `EventDispatcher` entoure chaque handler, logue et, après 10 erreurs/min sur une instance, la termine en `aborted`.
- Le rapport de match ne doit jamais être perdu : journal local si BDD KO (cf. `01-architecture.md` §3.3).

## 4. Logs

| Niveau | Usage |
|---|---|
| `error` | exception non gérée, perte de données possible |
| `warn` | dégradé mais géré (Redis reconnecté, config partielle) |
| `info` | cycle de vie (serveur prêt, instance créée/détruite, config publiée) — **jamais par joueur par tick** |
| `debug` | détails activables via `nexus.yml` → `debug: [instance, queue, combat]` |

Toujours inclure les identifiants : `instance=bw-7f3a match=0192… player=Maxence`.

## 5. i18n

- Fichiers `lang/<locale>.yml` par plugin, clés `domaine.sous.cle` ; MiniMessage avec placeholders nommés `<player>`, `<count>`.
- Locale du joueur : `players.locale` (choisie) sinon locale client, sinon `fr_FR`.
- Les clés de config (`@Setting(label=...)`) et de stats suivent la même mécanique.
- Test automatique : toutes les clés présentes en `fr_FR` existent en `en_US` (tâche Gradle `checkI18n`).

## 6. Tests

| Type | Où | Outils | Exigence |
|---|---|---|---|
| Unitaires | partout, en priorité logique pure (calculs, matchmaking, résolution des permissions, économie, TeamAssigner) | JUnit 5, AssertJ | ≥ 80 % sur `calc/`, `logic/`, `Matchmaker`, `PermissionResolver` |
| Jeux | `games/*/src/test` | `nexus-api-testkit` (`FakeGameContext`) | conditions de victoire, scoring |
| Intégration données | `nexus-data/src/integrationTest` | Testcontainers Postgres 17 + Valkey 8 | toutes les requêtes des repositories, migrations from scratch |
| Orchestrateur | `services/nexus-orchestrator/src/test` | faux serveurs (heartbeats simulés) | réconciliation, allocation, failover |
| Manuel en jeu | checklist de chaque spec de jeu | `docker compose` dev | avant chaque release |

Pas de MockBukkit imposé : vérifier au P0 s'il supporte 26.1 ; sinon on s'en passe grâce à la séparation logique pure / adaptateurs.

## 7. Clean code & Git

- Principes de conception, outils de qualité et checklist de relecture : **`15-clean-code.md`**.
- Branches, commits à chaque étape, format des messages, identité et absence d'attribution, hooks : **`16-git-workflow.md`**.
