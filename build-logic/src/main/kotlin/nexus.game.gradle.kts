// Module de mini-jeu : plugin Paper qui ne dépend que de nexus-api (cf. games/CLAUDE.md).
// La vérification verifyGameDependencies est ajoutée par P0-02.

plugins {
    id("nexus.paper-plugin")
}

dependencies {
    compileOnly(project(":platform:nexus-api"))
}
