plugins {
    id("nexus.java")
    id("nexus.shadow")
}

base {
    archivesName = "NexusProxy"
}

dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    implementation(project(":common:nexus-data"))
    implementation(project(":common:nexus-common"))
}
