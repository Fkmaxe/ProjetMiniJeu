// Module de mini-jeu : plugin Paper qui ne dépend que de nexus-api (cf. games/CLAUDE.md).
// Les dépendances autorisées (nexus-api en compileOnly, rien d'autre) sont vérifiées par verifyProjectDependencies.

plugins {
    id("nexus.paper-plugin")
}

dependencies {
    compileOnly(project(":platform:nexus-api"))
}
