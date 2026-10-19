plugins {
    id("nexus.paper-plugin")
    id("nexus.shadow")
}

base {
    archivesName = "NexusCore"
}

dependencies {
    implementation(project(":platform:nexus-api"))
    implementation(project(":platform:nexus-combat"))
    implementation(project(":common:nexus-data"))
    implementation(project(":common:nexus-common"))
}
