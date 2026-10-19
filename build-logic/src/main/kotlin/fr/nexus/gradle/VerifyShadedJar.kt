package fr.nexus.gradle

import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Échoue si le jar shadé contient du code tiers non relocalisé (cf. [ShadedJar]). */
@DisableCachingByDefault(because = "vérification rapide sans sortie")
abstract class VerifyShadedJar : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jar: RegularFileProperty

    @TaskAction
    fun verify() {
        val file = jar.get().asFile
        val violations = ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            val services = entries
                .filter { it.startsWith(SERVICES_DIR) && it.length > SERVICES_DIR.length }
                .associate { name ->
                    name.removePrefix(SERVICES_DIR) to zip.getInputStream(zip.getEntry(name)).bufferedReader()
                        .readLines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }
                }
            ShadedJar.violations(entries, services)
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Jar shadé non conforme à l'ADR-009 (${file.name}) :\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
    }

    private companion object {
        const val SERVICES_DIR = "META-INF/services/"
    }
}
