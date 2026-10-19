// Convention de base de tout module Java : toolchain 25, compilation stricte, JUnit + AssertJ, JSpecify.
// L'outillage qualité (Spotless, Error Prone, NullAway, Checkstyle, JaCoCo) est ajouté par P0-11.

plugins {
    `java-library`
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing", "-Werror"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnlyApi(libs.findLibrary("jspecify").get())

    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
