// Source set integrationTest (Testcontainers, Docker requis) : lancé par ./gradlew integrationTest, hors check.

plugins {
    id("nexus.java")
}

val libs = the<VersionCatalogsExtension>().named("libs")

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)
            dependencies {
                implementation(project())
                implementation(platform(libs.findLibrary("testcontainers-bom").get()))
                implementation(libs.findLibrary("testcontainers-junit").get())
                implementation(libs.findLibrary("assertj").get())
            }
        }
    }
}
