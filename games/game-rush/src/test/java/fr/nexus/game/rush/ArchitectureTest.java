package fr.nexus.game.rush;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import fr.nexus.archrules.GameArchRules;
import fr.nexus.archrules.NexusArchRules;

@AnalyzeClasses(packages = "fr.nexus.game.rush", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchTests NEXUS_RULES = ArchTests.in(NexusArchRules.class);

    @ArchTest
    static final ArchTests GAME_RULES = ArchTests.in(GameArchRules.class);
}
