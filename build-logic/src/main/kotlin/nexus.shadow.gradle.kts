// Jar shadé dont les dépendances tierces sont relocalisées sous fr.nexus.libs.* (cf. docs/02-project-structure.md §1).

plugins {
    id("com.gradleup.shadow")
}

val relocatedPackages = listOf(
    "com.zaxxer.hikari",
    "org.flywaydb",
    "io.lettuce",
    "io.netty",
    "com.github.benmanes.caffeine",
)

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier = ""
    relocatedPackages.forEach { relocate(it, "fr.nexus.libs.$it") }
}

tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}
