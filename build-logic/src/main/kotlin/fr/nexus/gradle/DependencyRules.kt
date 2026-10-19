package fr.nexus.gradle

/** Dépendances déclarées par un module : chemins de projets et coordonnées externes `groupe:nom`. */
data class DeclaredDependencies(
    val compileProjects: Set<String>,
    val runtimeProjects: Set<String>,
    val externals: Set<String>,
)

/**
 * Règle d'un module. [allowedExternals] à `null` autorise toute bibliothèque externe ;
 * [compileOnlyProjects] interdit aux dépendances projet d'atteindre le runtime.
 */
data class DependencyRule(
    val allowedProjects: Set<String>,
    val allowedExternals: Set<String>? = null,
    val compileOnlyProjects: Boolean = false,
)

/** Graphe autorisé entre modules — cf. docs/02-project-structure.md §1–2. */
object DependencyRules {
    private const val GAMES_PREFIX = ":games:"
    private const val COMMON = ":common:nexus-common"
    private const val DATA = ":common:nexus-data"
    private const val API = ":platform:nexus-api"
    private const val COMBAT = ":platform:nexus-combat"

    /** Un jeu ne voit que nexus-api, à la compilation, plus ce qu'apportent les conventions nexus.paper-plugin. */
    val gameRule = DependencyRule(
        allowedProjects = setOf(API),
        allowedExternals = setOf("io.papermc.paper:paper-api", "org.jspecify:jspecify"),
        compileOnlyProjects = true,
    )

    private val rulesByModule = mapOf(
        COMMON to DependencyRule(emptySet()),
        DATA to DependencyRule(setOf(COMMON)),
        API to DependencyRule(setOf(COMMON)),
        ":platform:nexus-api-testkit" to DependencyRule(setOf(API)),
        COMBAT to DependencyRule(setOf(API)),
        ":platform:nexus-core" to DependencyRule(setOf(API, COMBAT, DATA, COMMON)),
        ":platform:nexus-lobby" to DependencyRule(setOf(API)),
        ":proxy:nexus-proxy" to DependencyRule(setOf(DATA, COMMON)),
        ":services:nexus-orchestrator" to DependencyRule(setOf(DATA, COMMON)),
    )

    fun ruleFor(projectPath: String): DependencyRule? =
        if (projectPath.startsWith(GAMES_PREFIX)) gameRule else rulesByModule[projectPath]

    /** Liste les violations des règles par [projectPath] ; vide si le module est conforme. */
    fun violations(projectPath: String, declared: DeclaredDependencies): List<String> {
        val rule = ruleFor(projectPath)
            ?: return listOf("$projectPath n'a pas de règle de dépendance : le déclarer dans DependencyRules")
        return forbiddenProjects(rule, declared) + forbiddenExternals(rule, declared) + runtimeProjects(rule, declared)
    }

    private fun forbiddenProjects(rule: DependencyRule, declared: DeclaredDependencies): List<String> =
        (declared.compileProjects + declared.runtimeProjects - rule.allowedProjects)
            .sorted()
            .map { "dépendance projet interdite : $it (autorisées : ${rule.allowedProjects.sorted()})" }

    private fun forbiddenExternals(rule: DependencyRule, declared: DeclaredDependencies): List<String> {
        val allowed = rule.allowedExternals ?: return emptyList()
        return (declared.externals - allowed)
            .sorted()
            .map { "bibliothèque externe interdite : $it (autorisées : ${allowed.sorted()})" }
    }

    private fun runtimeProjects(rule: DependencyRule, declared: DeclaredDependencies): List<String> =
        if (!rule.compileOnlyProjects) emptyList()
        else declared.runtimeProjects.intersect(rule.allowedProjects).sorted()
            .map { "$it doit être déclaré en compileOnly (fourni à l'exécution par le serveur)" }
}
