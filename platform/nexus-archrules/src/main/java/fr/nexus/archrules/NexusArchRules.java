package fr.nexus.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Règles valables dans tous les modules ; chaque {@code ArchitectureTest} les inclut via {@code ArchTests.in}. */
public final class NexusArchRules {

    @ArchTest
    public static final ArchRule NO_SERVER_INTERNALS = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft..", "org.bukkit.craftbukkit..")
            .because("NMS et CraftBukkit sont interdits (ADR-006)");

    @ArchTest
    public static final ArchRule PURE_RULES_WITHOUT_SERVER_API = noClasses()
            .that()
            .resideInAnyPackage("..logic..", "..calc..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.bukkit..", "io.papermc..", "com.velocitypowered..")
            .because("la logique de règles est du Java pur testable (CLAUDE.md règle 9)");

    @ArchTest
    public static final ArchRule JDBC_ONLY_IN_DATA = noClasses()
            .that()
            .resideOutsideOfPackage("fr.nexus.data..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("java.sql..", "javax.sql..")
            .because("l'accès BDD passe par les repositories de nexus-data");

    @ArchTest
    public static final ArchRule REPOSITORIES_IN_REPO_PACKAGE = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .resideInAPackage("..repo..")
            .because("les repositories vivent dans fr.nexus.data.repo (docs/02-project-structure.md §3)");

    @ArchTest
    public static final ArchRule NO_PACKAGE_CYCLES =
            slices().matching("fr.nexus.(**)").should().beFreeOfCycles();

    private NexusArchRules() {}
}
