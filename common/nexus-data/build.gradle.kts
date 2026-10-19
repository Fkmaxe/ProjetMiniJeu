plugins {
    id("nexus.java")
    id("nexus.integration-test")
}

dependencies {
    api(project(":common:nexus-common"))
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.postgres)
    implementation(libs.lettuce)
    implementation(libs.caffeine)

    "integrationTestImplementation"(libs.testcontainers.postgres)
    "integrationTestImplementation"(libs.testcontainers.core)
}
