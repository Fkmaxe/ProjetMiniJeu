// Convention de base de tout module Java : toolchain 25, compilation stricte, JUnit + AssertJ, JSpecify.
// Outillage qualité (docs/15-clean-code.md §2) : Spotless, Error Prone + NullAway, Checkstyle, JaCoCo.

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    checkstyle
    jacoco
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat(libs.findVersion("palantir-format").get().requiredVersion)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = libs.findVersion("checkstyle").get().requiredVersion
    configFile = layout.settingsDirectory.file("config/checkstyle/nexus.xml").asFile
    maxWarnings = 0
}

// Règles de conception sur le code de production ; les tests gardent leurs tables de valeurs de référence.
tasks.withType<Checkstyle>().matching { it.name != "checkstyleMain" }.configureEach {
    enabled = false
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -Werror rend aussi bloquants les avertissements Error Prone, dans tous les modules.
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
    }
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnlyApi(libs.findLibrary("jspecify").get())
    "errorprone"(libs.findLibrary("errorprone-core").get())
    "errorprone"(libs.findLibrary("nullaway").get())

    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Couverture exigée sur la logique pure (docs/15-clean-code.md §2) ; le reste est mesuré sans seuil.
val coverageMinimum = "0.80".toBigDecimal()
val coveredClasses = listOf("*.calc.*", "*.logic.*", "*.Matchmaker", "*.PermissionResolver")

jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            element = "CLASS"
            includes = coveredClasses
            limit {
                counter = "LINE"
                minimum = coverageMinimum
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

// Graphe de dépendances entre modules (code principal uniquement, les suites de test sont libres).
val verifyProjectDependencies = tasks.register<fr.nexus.gradle.VerifyProjectDependencies>("verifyProjectDependencies") {
    group = "verification"
    description = "Vérifie les dépendances du module contre docs/02-project-structure.md."
    val selfPath = project.path
    val compileConfigurations = listOf("compileClasspath", "annotationProcessor").map { configurations.getByName(it) }
    val runtimeConfiguration = listOf(configurations.getByName("runtimeClasspath"))
    projectPath = selfPath
    compileProjects = provider { fr.nexus.gradle.projectPathsOf(compileConfigurations, selfPath) }
    runtimeProjects = provider { fr.nexus.gradle.projectPathsOf(runtimeConfiguration, selfPath) }
    externals = provider { fr.nexus.gradle.externalCoordinatesOf(compileConfigurations + runtimeConfiguration) }
}

tasks.named("check") {
    dependsOn(verifyProjectDependencies)
}
