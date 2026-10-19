// Application autonome : jar shadé SANS relocalisation (exception de docs/02-project-structure.md §1),
// d'où Shadow appliqué directement (fourni par le classpath de build-logic) plutôt que nexus.shadow.

plugins {
    id("nexus.java")
    id("nexus.integration-test")
    application
}

apply(plugin = "com.gradleup.shadow")

application {
    mainClass = "fr.nexus.orchestrator.Main"
}

dependencies {
    implementation(project(":common:nexus-data"))
    implementation(project(":common:nexus-common"))
    implementation(libs.dockerjava.core)
    implementation(libs.dockerjava.httpclient5)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback)
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier = ""
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.assemble {
    dependsOn("shadowJar")
}
