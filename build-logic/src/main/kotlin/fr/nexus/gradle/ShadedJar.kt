package fr.nexus.gradle

/** Contenu attendu d'un jar shadé de plugin (ADR-009) : tout le code tiers relocalisé sous fr.nexus.libs. */
object ShadedJar {
    const val RELOCATION_PREFIX = "fr.nexus.libs"

    /** Racines des bibliothèques embarquées par nexus-data (Hikari, Flyway, Postgres, Lettuce, Caffeine) et leurs dépendances. */
    val relocatedPackages = listOf(
        "com.zaxxer.hikari",
        "org.flywaydb",
        "org.postgresql",
        "io.lettuce",
        "io.netty",
        "reactor",
        "io.micrometer",
        "org.reactivestreams",
        "redis.clients",
        "com.github.benmanes.caffeine",
        "com.fasterxml.jackson",
    )

    /** Fournies par Paper/Velocity à l'exécution (SLF4J) ou utiles seulement à la compilation (annotations). */
    val excludedModules = listOf(
        "org.slf4j:slf4j-api",
        "org.checkerframework:checker-qual",
        "com.google.errorprone:error_prone_annotations",
        "org.jspecify:jspecify",
    )

    private const val OWN_CLASSES = "fr/nexus/"
    private const val OWN_PACKAGE = "fr.nexus."
    private val jdkServicePrefixes = listOf("java.", "javax.")
    private val multiReleasePrefix = Regex("^META-INF/versions/\\d+/")

    /** Liste les entrées non relocalisées : classes hors fr/nexus/ et services qui nomment une classe tierce. */
    fun violations(entries: Collection<String>, services: Map<String, List<String>>): List<String> =
        foreignClassPackages(entries).map { "classes non relocalisées : $it" } +
            services.flatMap { (name, implementations) -> serviceViolations(name, implementations) }

    private fun foreignClassPackages(entries: Collection<String>): List<String> =
        entries.asSequence()
            .filter { it.endsWith(".class") }
            .map { it.replace(multiReleasePrefix, "") }
            .filterNot { it.startsWith(OWN_CLASSES) }
            .map { it.substringBeforeLast('/') }
            .distinct()
            .sorted()
            .toList()

    private fun serviceViolations(name: String, implementations: List<String>): List<String> {
        val serviceOk = name.startsWith(OWN_PACKAGE) || jdkServicePrefixes.any { name.startsWith(it) }
        val nameViolation = if (serviceOk) emptyList() else listOf("service non relocalisé : $name")
        return nameViolation + implementations
            .filterNot { it.startsWith(OWN_PACKAGE) }
            .map { "implémentation non relocalisée dans $name : $it" }
    }
}
