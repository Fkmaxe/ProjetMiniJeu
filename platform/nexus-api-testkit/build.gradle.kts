plugins {
    id("nexus.java")
}

dependencies {
    api(project(":platform:nexus-api"))
    compileOnly(libs.paper.api)
}
