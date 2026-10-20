package fr.nexus.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ShadedJarTest {
    @Test
    fun violations_onlyOwnAndRelocatedClasses_isEmpty() {
        val entries = listOf(
            "fr/nexus/core/bootstrap/NexusCorePlugin.class",
            "fr/nexus/libs/com/zaxxer/hikari/HikariConfig.class",
            "META-INF/versions/11/fr/nexus/libs/reactor/core/Scannable.class",
            "paper-plugin.yml",
        )
        val services = mapOf("java.sql.Driver" to listOf("fr.nexus.libs.org.postgresql.Driver"))

        assertThat(ShadedJar.violations(entries, services)).isEmpty()
    }

    @Test
    fun violations_unrelocatedClass_reportsItsPackage() {
        val result = ShadedJar.violations(listOf("reactor/core/publisher/Flux.class"), emptyMap())

        assertThat(result).singleElement().asString().contains("reactor/core/publisher")
    }

    @Test
    fun violations_unrelocatedMultiReleaseClass_reportsItsPackage() {
        val result = ShadedJar.violations(listOf("META-INF/versions/21/reactor/core/Scannable.class"), emptyMap())

        assertThat(result).singleElement().asString().contains("reactor/core")
    }

    @Test
    fun violations_unrelocatedServiceName_reportsService() {
        val services = mapOf("org.flywaydb.core.extensibility.Plugin" to listOf("fr.nexus.libs.org.flywaydb.X"))

        assertThat(ShadedJar.violations(emptyList(), services)).singleElement().asString().contains("service")
    }

    @Test
    fun violations_unrelocatedImplementation_reportsImplementation() {
        val services = mapOf("java.sql.Driver" to listOf("org.postgresql.Driver"))

        assertThat(ShadedJar.violations(emptyList(), services)).singleElement().asString().contains("org.postgresql.Driver")
    }
}
