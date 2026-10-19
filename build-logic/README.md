# build-logic — plugins de convention Gradle

Build inclus (`includeBuild("build-logic")`) qui centralise la configuration de tous les modules.
Il importe `../gradle/libs.versions.toml` (catalogue `libs`) et embarque les plugins Shadow et run-paper sur son classpath.
**Créé par la tâche P0-01**, outillage qualité ajouté par P0-11, hooks par P0-12.

| Plugin (id) | Fichier | Rôle | Livré par P0-01 | Complété par |
|---|---|---|---|---|
| `nexus.java` | `src/main/kotlin/nexus.java.gradle.kts` | socle de tout module Java | toolchain Java 25, UTF-8, `-parameters -Xlint:all,-processing -Werror`, JSpecify (`compileOnlyApi`), JUnit 6 + AssertJ ; P0-02 : `verifyProjectDependencies` dans `check` | P0-11 : Spotless, Error Prone + NullAway, Checkstyle (`config/checkstyle/nexus.xml`), JaCoCo |
| `nexus.paper-plugin` | `nexus.paper-plugin.gradle.kts` | plugin Paper | `nexus.java` + `paper-api` compileOnly + `${version}` injecté dans `paper-plugin.yml` + `runServer` (run-paper, `minecraftVersion`) | validation de `paper-plugin.yml` |
| `nexus.game` | `nexus.game.gradle.kts` | mini-jeu | `nexus.paper-plugin` + `nexus-api` compileOnly | — (règle « jeu » de `DependencyRules`) |
| `nexus.shadow` | `nexus.shadow.gradle.kts` | jar shadé | Shadow, jar principal sans classifier (jar brut : `-plain`), relocalisations `fr.nexus.libs.*` (Hikari, Flyway, Lettuce, Netty, Caffeine) | P0-03 : shadow vs loader Paper (ADR-009), Reactor non relocalisé à ce jour |
| `nexus.integration-test` | `nexus.integration-test.gradle.kts` | tests Testcontainers | suite `integrationTest` (JUnit + Testcontainers 2), **hors** `check` (Docker requis) | — |

L'orchestrateur (application autonome, rien n'est relocalisé) applique Shadow directement par
`apply(plugin = "com.gradleup.shadow")` au lieu de `nexus.shadow` : seul un plugin précompilé peut être
résolu par id depuis le build inclus.

## Règles de dépendance (P0-02)

`fr/nexus/gradle/DependencyRules.kt` contient la table du graphe autorisé (`docs/02-project-structure.md` §2) et une
fonction pure testée (`src/test/kotlin`). `VerifyProjectDependencies` l'applique à chaque module ;
`./gradlew checkDependencyRules` (racine) les agrège. **Un nouveau module doit être ajouté à la table** ; un nouveau
jeu `:games:*` hérite automatiquement de la règle « jeu ». Les tests de build-logic tournent dans `./gradlew check`.

Package Kotlin : `fr.nexus.gradle` (pas `fr.nexus.build`, que le motif `build/` du `.gitignore` exclurait).

Tâches racine restant à définir dans le `build.gradle.kts` racine : `checkI18n`,
`collectJars`, `installGitHooks`, `packageRelease` (archives par type de serveur pour Pterodactyl, cf. `docs/17-deploiement-pterodactyl.md`).
