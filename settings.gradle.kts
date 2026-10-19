// Structure du monorepo Nexus — cf. docs/02-project-structure.md
// Chaque module applique un plugin de convention de build-logic (cf. build-logic/README.md).

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
    ":platform:nexus-archrules",
    ":platform:nexus-combat",
    ":platform:nexus-core",
    ":platform:nexus-lobby",
    ":proxy:nexus-proxy",
    ":services:nexus-orchestrator",
    ":games:game-dropper",
    ":games:game-rush",
    ":games:game-bedwars",
)
