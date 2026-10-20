// Jar shadé de plugin : dépendances tierces relocalisées sous fr.nexus.libs.* (ADR-009, docs/02-project-structure.md §1).

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import fr.nexus.gradle.ShadedJar
import fr.nexus.gradle.VerifyShadedJar

plugins {
    id("com.gradleup.shadow")
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = ""
    // Flyway et le driver JDBC sont découverts par ServiceLoader : fusionner puis relocaliser les fichiers de services.
    mergeServiceFiles()
    ShadedJar.relocatedPackages.forEach { relocate(it, "${ShadedJar.RELOCATION_PREFIX}.$it") }
    dependencies {
        ShadedJar.excludedModules.forEach { exclude(dependency(it)) }
    }
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
}

tasks.named("assemble") {
    dependsOn(shadowJar)
}

val verifyShadedJar = tasks.register<VerifyShadedJar>("verifyShadedJar") {
    group = "verification"
    description = "Vérifie que le jar shadé ne contient aucune classe tierce non relocalisée (ADR-009)."
    jar = shadowJar.flatMap { it.archiveFile }
}

tasks.named("check") {
    dependsOn(verifyShadedJar)
}
