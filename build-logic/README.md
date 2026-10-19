# build-logic — plugins de convention Gradle

Build inclus (`includeBuild("build-logic")`) qui centralise la configuration de tous les modules.
**Créé par la tâche P0-01**, outillage qualité ajouté par P0-11, hooks par P0-12.

| Plugin (id) | Fichier attendu | Rôle |
|---|---|---|
| `nexus.java` | `src/main/kotlin/nexus.java.gradle.kts` | toolchain Java 25, encodage, JUnit 5/AssertJ, JSpecify, Spotless, Error Prone + NullAway, Checkstyle (`config/checkstyle/nexus.xml`), JaCoCo |
| `nexus.paper-plugin` | `nexus.paper-plugin.gradle.kts` | `nexus.java` + `paper-api` compileOnly + `paper-plugin.yml` + run-paper |
| `nexus.game` | `nexus.game.gradle.kts` | `nexus.paper-plugin` + `nexus-api` compileOnly + `verifyGameDependencies` |
| `nexus.shadow` | `nexus.shadow.gradle.kts` | Shadow + relocalisations `fr.nexus.libs.*` |
| `nexus.integration-test` | `nexus.integration-test.gradle.kts` | source set `integrationTest` + Testcontainers |

Tâches racine à définir ici ou dans le `build.gradle.kts` racine : `checkDependencyRules`, `checkI18n`,
`collectJars`, `installGitHooks`, `packageRelease` (archives par type de serveur pour Pterodactyl, cf. `docs/17-deploiement-pterodactyl.md`).
