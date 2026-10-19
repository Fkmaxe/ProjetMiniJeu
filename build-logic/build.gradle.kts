plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.run.paper.gradle.plugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
