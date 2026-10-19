package fr.nexus.api.testkit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import fr.nexus.archrules.NexusArchRules;

@AnalyzeClasses(packages = "fr.nexus.api.testkit", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchTests NEXUS_RULES = ArchTests.in(NexusArchRules.class);
}
