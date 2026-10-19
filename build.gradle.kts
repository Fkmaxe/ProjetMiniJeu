// Projet racine : tâches transverses du monorepo (cf. build-logic/README.md).

plugins {
    base
}

tasks.register("checkDependencyRules") {
    group = "verification"
    description = "Vérifie le graphe de dépendances de tous les modules (docs/02-project-structure.md §2)."
    // Les projets intermédiaires (:common, :platform…) ne sont que des dossiers : seuls les modules feuilles sont vérifiés.
    dependsOn(subprojects.filter { it.childProjects.isEmpty() }.map { "${it.path}:verifyProjectDependencies" })
}

tasks.check {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
