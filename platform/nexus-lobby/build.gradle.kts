plugins {
    id("nexus.paper-plugin")
}

base {
    archivesName = "NexusLobby"
}

dependencies {
    compileOnly(project(":platform:nexus-api"))
}
