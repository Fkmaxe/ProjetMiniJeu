// Structure du monorepo Nexus — cf. docs/02-project-structure.md
// Les dossiers de modules contiennent pour l'instant un README.md ; le build.gradle.kts
// de chacun est créé par la tâche P0-01.

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "nexus"

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
