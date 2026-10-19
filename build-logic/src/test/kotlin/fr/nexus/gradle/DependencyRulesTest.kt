package fr.nexus.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependencyRulesTest {
    private val api = ":platform:nexus-api"
    private val paper = "io.papermc.paper:paper-api"

    private fun declared(
        compile: Set<String> = emptySet(),
        runtime: Set<String> = emptySet(),
        externals: Set<String> = emptySet(),
    ) = DeclaredDependencies(compile, runtime, externals)

    @Test
    fun violations_gameWithApiCompileOnly_isEmpty() {
        val result = DependencyRules.violations(":games:game-dropper", declared(compile = setOf(api), externals = setOf(paper)))

        assertThat(result).isEmpty()
    }

    @Test
    fun violations_gameDependingOnCore_reportsForbiddenProject() {
        val result = DependencyRules.violations(":games:game-rush", declared(compile = setOf(api, ":platform:nexus-core")))

        assertThat(result).singleElement().asString().contains(":platform:nexus-core")
    }

    @Test
    fun violations_gameWithExternalLibrary_reportsForbiddenExternal() {
        val result = DependencyRules.violations(":games:game-bedwars", declared(externals = setOf(paper, "com.google.code.gson:gson")))

        assertThat(result).singleElement().asString().contains("com.google.code.gson:gson")
    }

    @Test
    fun violations_gameWithApiAtRuntime_reportsCompileOnlyRequired() {
        val result = DependencyRules.violations(":games:game-dropper", declared(compile = setOf(api), runtime = setOf(api)))

        assertThat(result).singleElement().asString().contains("compileOnly")
    }

    @Test
    fun violations_newGameModule_usesGameRule() {
        val result = DependencyRules.violations(":games:game-foo", declared(compile = setOf(":common:nexus-data")))

        assertThat(result).singleElement().asString().contains(":common:nexus-data")
    }

    @Test
    fun violations_lobbyDependingOnCombat_reportsForbiddenProject() {
        val result = DependencyRules.violations(":platform:nexus-lobby", declared(compile = setOf(api, ":platform:nexus-combat")))

        assertThat(result).singleElement().asString().contains(":platform:nexus-combat")
    }

    @Test
    fun violations_commonDependingOnProject_reportsForbiddenProject() {
        val result = DependencyRules.violations(":common:nexus-common", declared(compile = setOf(api)))

        assertThat(result).hasSize(1)
    }

    @Test
    fun violations_coreWithItsGraphAndLibraries_isEmpty() {
        val graph = setOf(api, ":platform:nexus-combat", ":common:nexus-data", ":common:nexus-common")
        val result = DependencyRules.violations(
            ":platform:nexus-core",
            declared(compile = graph, runtime = graph, externals = setOf(paper, "com.zaxxer:HikariCP")),
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun violations_unknownModule_reportsMissingRule() {
        val result = DependencyRules.violations(":platform:nexus-unknown", declared())

        assertThat(result).singleElement().asString().contains("pas de règle")
    }
}
