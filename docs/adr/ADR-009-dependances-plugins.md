# ADR-009 — Embarquer les dépendances (Shadow) ou les charger (loader Paper)

- **Statut** : accepté (tâche P0-03)
- **Date** : 2026-10-19

## Question
Comment fournir HikariCP, Flyway, le driver Postgres, Lettuce et Caffeine à `nexus-core` / `nexus-lobby` ?

## Options
1. **Shadow + relocalisation** `fr.nexus.libs.*` : jar autonome, aucune requête réseau au démarrage, jars plus gros.
2. **Loader de bibliothèques Paper** (`PluginLoader` + `MavenLibraryResolver` dans `paper-plugin.yml`) : jars légers, téléchargement au premier démarrage (nécessite l'accès à Maven Central depuis les serveurs Pterodactyl).

## Critères
Démarrage hors-ligne possible sur Pterodactyl, absence de conflit avec d'autres plugins, taille des archives de release, simplicité.

## Décision
**Option 1 — Shadow + relocalisation** pour tous les jars de plugins qui embarquent des bibliothèques (`nexus-core`, `nexus-proxy`).

- **Démarrage hors ligne** : un serveur Pterodactyl démarre sans joindre Maven Central ; le loader Paper télécharge au
  premier démarrage et échoue si le dépôt est inaccessible (critère éliminatoire).
- **Pas de conflit** : tout le code tiers est relocalisé sous `fr.nexus.libs.*` — Hikari, Flyway, driver Postgres,
  Lettuce et ses dépendances (Netty, Reactor, reactive-streams, `redis.clients`, Micrometer), Caffeine, Jackson.
- **Taille** : `NexusCore` ≈ 11,4 Mo (mesuré le 2026-10-19), acceptable pour des archives de release téléchargées une fois par version.
- **Simplicité** : un seul mécanisme, vérifiable au build.

## Mise en œuvre
- Convention `nexus.shadow` (`build-logic`) : liste des relocalisations et exclusions dans `fr.nexus.gradle.ShadedJar`.
- `mergeServiceFiles()` **obligatoire** : Flyway (`database-postgresql`) et le driver JDBC sont découverts par `ServiceLoader` ;
  les fichiers de services sont fusionnés puis relocalisés.
- Exclus du jar : SLF4J (fourni par Paper et Velocity), annotations de compilation (Checker Framework, Error Prone, JSpecify),
  `module-info` et signatures.
- Tâche `verifyShadedJar` (dans `check`) : échoue si une classe hors `fr/nexus/` ou un service non relocalisé apparaît —
  une nouvelle dépendance transitive impose donc une décision explicite (relocaliser ou exclure).
- `nexus-orchestrator` (application autonome) reste shadé **sans** relocalisation.

## Conséquences
- Les natifs Netty (epoll/kqueue) ne sont pas chargés sous leur nom relocalisé : Lettuce utilise le transport NIO, suffisant pour notre charge Redis.
- Toute montée de version d'une bibliothèque embarquée est vérifiée par `verifyShadedJar`.
