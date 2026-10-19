// Règles ArchUnit partagées (docs/15-clean-code.md §2), utilisées par l'ArchitectureTest de chaque module.

plugins {
    id("nexus.java")
}

dependencies {
    api(libs.archunit.junit5)
}
