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

// Hooks Git versionnés (docs/16-git-workflow.md §5) : copiés dans .git/hooks, sans toucher à la config Git.
val gitHooksDir = layout.projectDirectory.dir(".git/hooks")

val installGitHooks = tasks.register<Copy>("installGitHooks") {
    group = "build setup"
    description = "Installe les hooks Git du dépôt (.githooks/) dans .git/hooks/."
    from(layout.projectDirectory.dir(".githooks")) {
        include("commit-msg", "pre-commit")
    }
    into(gitHooksDir)
    filePermissions { unix("rwxr-xr-x") }
    val hooksDirFile = gitHooksDir.asFile
    onlyIf("dépôt Git présent") { hooksDirFile.parentFile.isDirectory }
}

tasks.build {
    dependsOn(installGitHooks)
}
