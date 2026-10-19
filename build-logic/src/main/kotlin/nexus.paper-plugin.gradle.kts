// Plugin Paper : nexus.java + paper-api fourni par le serveur + version injectée dans paper-plugin.yml + run-paper.

plugins {
    id("nexus.java")
    id("xyz.jpenilla.run-paper")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val minecraftVersion = providers.gradleProperty("minecraftVersion").get()

dependencies {
    compileOnly(libs.findLibrary("paper-api").get())
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("paper-plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion)
}
