package fr.nexus.archrules;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Règles propres aux modules de jeu (games/CLAUDE.md). */
public final class GameArchRules {

    @ArchTest
    public static final ArchRule ONLY_NEXUS_API = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("fr.nexus.core..", "fr.nexus.data..", "fr.nexus.combat..", "fr.nexus.lobby..")
            .because("un jeu ne passe que par nexus-api (CLAUDE.md règle 5)");

    @ArchTest
    public static final ArchRule NO_GLOBAL_PLAYER_LIST = noClasses()
            .should()
            .callMethod("org.bukkit.Bukkit", "getOnlinePlayers")
            .because("la logique de jeu est scopée à l'instance : instance.players() (CLAUDE.md règle 7)");

    @ArchTest
    public static final ArchRule NO_DIRECT_LISTENER_REGISTRATION = noClasses()
            .should()
            .callMethodWhere(target(name("registerEvents")))
            .because("les listeners de jeu passent par GameContext#events() (CLAUDE.md règle 8)");

    private GameArchRules() {}
}
