plugins {
    id("nexus.java")
}

dependencies {
    compileOnly(project(":platform:nexus-api"))
    compileOnly(libs.paper.api)
}
