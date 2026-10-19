plugins {
    id("nexus.java")
}

dependencies {
    api(project(":common:nexus-common"))
    compileOnly(libs.paper.api)
}
