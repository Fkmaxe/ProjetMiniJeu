package fr.nexus.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Échoue si le module viole son entrée de [DependencyRules]. Entrées calculées à la configuration. */
@DisableCachingByDefault(because = "vérification instantanée sans sortie")
abstract class VerifyProjectDependencies : DefaultTask() {
    @get:Input abstract val projectPath: Property<String>
    @get:Input abstract val compileProjects: SetProperty<String>
    @get:Input abstract val runtimeProjects: SetProperty<String>
    @get:Input abstract val externals: SetProperty<String>

    @TaskAction
    fun verify() {
        val declared = DeclaredDependencies(compileProjects.get(), runtimeProjects.get(), externals.get())
        val violations = DependencyRules.violations(projectPath.get(), declared)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Règles de dépendance violées par ${projectPath.get()} (docs/02-project-structure.md) :\n" +
                    violations.joinToString("\n") { "  - $it" },
            )
        }
    }
}

/** Chemins des projets déclarés dans [configurations], hors le module lui-même. */
fun projectPathsOf(configurations: Iterable<Configuration>, selfPath: String): Set<String> =
    configurations.flatMap { it.allDependencies.withType(ProjectDependency::class.java) }
        .map { it.path }
        .filterNot { it == selfPath }
        .toSet()

/** Coordonnées `groupe:nom` des bibliothèques externes déclarées dans [configurations]. */
fun externalCoordinatesOf(configurations: Iterable<Configuration>): Set<String> =
    configurations.flatMap { it.allDependencies.withType(ExternalModuleDependency::class.java) }
        .map { "${it.group}:${it.name}" }
        .toSet()
