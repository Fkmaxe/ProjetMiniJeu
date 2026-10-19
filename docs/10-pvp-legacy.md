# 10 — PvP « 1.8 » sur Paper 26.1 (`nexus-combat`)

Objectif : retrouver le **ressenti** du combat 1.8 (clic sans cooldown, combos, knockback, block-hit, dégâts 1.8)
sur des clients 26.1, sans NMS, en gardant le reste du jeu moderne. Activé par jeu via `LegacyCombatModule`
(Rush, Bedwars) et paramétrable par l'admin (profils de combat et de knockback).

## 1. Architecture

```
nexus-combat
├─ calc/                 (Java pur, 100 % testé)
│  ├─ LegacyDamageCalculator   dégâts d'arme, crit, enchantements, effets, armure, protection, blocage
│  ├─ KnockbackCalculator      formule 1.8 paramétrable
│  └─ LegacyTables             valeurs 1.8 (armes, armures, potions, pommes)
├─ apply/
│  ├─ DamageApplier            stratégie retenue au spike S2 (§6)
│  └─ KnockbackApplier
├─ listeners/            (adaptateurs Bukkit → calc, enregistrés via le bus scopé de l'instance)
└─ LegacyCombatModule    (GameModule : enable/disable par instance, applique le profil)
```

Le module ne touche **que** les joueurs de l'instance où il est actif (attributs remis à la valeur moderne à la sortie de l'instance).

## 2. Mécaniques à reproduire

| # | Mécanique | 1.8 | Moderne (à neutraliser) | Implémentation 26.1 |
|---|---|---|---|---|
| C1 | Cooldown d'attaque | aucun | barre de recharge, dégâts réduits si spam | attribut joueur `ATTACK_SPEED` base = 1024 à l'entrée dans l'instance (restauré à la sortie) |
| C2 | Dégâts des armes | épée bois/or 5, pierre 6, fer 7, diamant 8 ; hache 4/5/6/7 ; pioche 3/4/5/6 ; pelle 2/3/4/5 ; main 1 | valeurs 1.9+ | `LegacyTables` ; dégâts recalculés (§3). Netherite = diamant +1 (choix de design, configurable) |
| C3 | Coup critique | ×1,5 si en chute, pas au sol, pas dans l'eau/échelle, pas aveuglé ; **sprint autorisé** | sprint annule le crit | recalcul dans le calculateur à partir de l'état du joueur |
| C4 | Attaque balayage | inexistante | sweep | annuler `EntityDamageByEntityEvent` cause `ENTITY_SWEEP_ATTACK` |
| C5 | Tranchant | +1,25 / niveau | 0,5×niv + 0,5 | calculateur |
| C6 | Force / Faiblesse | +130 % / niveau ; −0,5 / niveau | +3 / niveau ; −4 | calculateur |
| C7 | Armure | réduction = points × 4 % (max 80 %), pas de robustesse | formule avec robustesse | calculateur (EPF protection 1.8 inclus) |
| C8 | Invulnérabilité | 10 ticks effectifs (`maximumNoDamageTicks` 20, coup plus fort pendant l'invul. = différence) | identique | garder ; `maximumNoDamageTicks` configurable (profil « combo » possible) |
| C9 | Knockback | formule §4, reset de sprint par W-tap | légèrement différente, KB resistance | override via `EntityKnockbackEvent`/`EntityKnockbackByEntityEvent` de Paper (`setKnockback(Vector)`) |
| C10 | Block-hit (épée) | clic droit = blocage, dégâts `(1 + d) × 0,5`, animation épée | inexistant (bouclier) | **spike S1** (§6) : composant `minecraft:blocks_attacks` sur les épées ; réduction appliquée par notre calculateur |
| C11 | Bouclier / main secondaire | inexistants | présents | bouclier retiré des boutiques/kits ; swap main secondaire annulé (`PlayerSwapHandItemsEvent`, clic slot 40) — option config |
| C12 | Régénération naturelle | +1 PV / 80 ticks si faim ≥ 18, épuisement +3 | régénération rapide par saturation | gamerule `naturalRegeneration` false dans le monde de l'instance + tâche par instance (toutes les 20 ticks, vérifie le timer de chaque joueur vivant) |
| C13 | Pomme d'or | Régén II 5 s + Absorption I 2 min | Régén II 5 s + Absorption I 2 min | identique (vérifier) ; **pomme enchantée** 1.8 : Régén V 30 s, Résistance I 5 min, Résistance au feu I 5 min, Absorption I 2 min → `PlayerItemConsumeEvent` remplace les effets |
| C14 | Perle de l'Ender | pas de cooldown | 1 s | `player.setCooldown(ENDER_PEARL, 0)` après lancer (option) |
| C15 | Canne à pêche | touche = 0 dégât + knockback | tire la cible | option `rodKnockback` : `ProjectileHitEvent` → appliquer KB 1.8 depuis le bouchon |
| C16 | Boules de neige / œufs | knockback sur joueurs | selon version | option `projectileKnockback` |
| C17 | Arcs | Puissance +25 %/niv, Frappe | identique | rien |
| C18 | Infobulles | — | attributs 1.9 affichés | composant `tooltip_display` masquant `ATTRIBUTE_MODIFIERS` + lore « +7 dégâts » généré par `LegacyItems` |

## 3. Calcul des dégâts (Java pur)

```java
public record AttackInput(WeaponType weapon, int sharpness, int strengthLevel, int weaknessLevel,
                          boolean critical, ArmorSet victimArmor, int victimProtectionEpf,
                          int victimResistanceLevel, boolean victimBlocking) {}

public final class LegacyDamageCalculator {
    public double compute(AttackInput in, CombatProfile p) {
        double d = LegacyTables.baseDamage(in.weapon());            // C2 (inclut le +1 de la main)
        d += in.strengthLevel() * 1.3 * d;                          // C6 (1.8 : multiplicatif sur la base)
        d -= in.weaknessLevel() * 0.5;
        if (in.critical()) d *= 1.5;                                // C3
        d += in.sharpness() * 1.25;                                 // C5
        if (in.victimBlocking()) d = (1 + d) * 0.5;                 // C10
        d *= 1 - Math.min(20, in.victimArmor().points()) * 0.04;    // C7
        d *= 1 - LegacyTables.protectionReduction(in.victimProtectionEpf(), p.random());
        d *= 1 - Math.min(5, in.victimResistanceLevel()) * 0.2;
        return Math.max(0, d);
    }
}
```

L'ordre des étapes suit le code 1.8 et **doit** être couvert par des tests de référence (table attendue `weapon × armure × crit × force`).
Les valeurs de `LegacyTables` sont documentées en commentaire avec leur source (wiki des versions 1.8).

## 4. Knockback (formule 1.8 paramétrable)

```
entrées : vitesse actuelle v de la victime, direction attaquant→victime (dx, dz) normalisée, kbLevel = enchant Recul + (attaquant sprinte ? 1 : 0)
v.x = v.x / friction - dx * horizontal
v.y = v.y / friction + vertical          ; si v.y > verticalLimit → v.y = verticalLimit
v.z = v.z / friction - dz * horizontal
si kbLevel > 0 :
    v.x += -sin(yawAttaquant) * kbLevel * extraHorizontal
    v.y += extraVertical
    v.z +=  cos(yawAttaquant) * kbLevel * extraHorizontal
    attaquant : vitesse horizontale × 0,6, sprint = false   (reset de sprint 1.8)
si victime au sol ou option "airKnockback" : appliquer ; sinon facteur airMultiplier
```

`KnockbackProfile` (éditable dans l'UI admin, presets « 1.8 vanilla », « Combo », « Lourd ») :

| Champ | 1.8 vanilla |
|---|---|
| `horizontal` | 0.4 |
| `vertical` | 0.4 |
| `verticalLimit` | 0.4 |
| `extraHorizontal` | 0.5 |
| `extraVertical` | 0.1 |
| `friction` | 2.0 |
| `airMultiplier` | 1.0 |
| `ignoreKnockbackResistance` | true |

Remarque netcode : la vitesse finale est envoyée au client par le serveur ; tester avec 50/100/150 ms de latence simulée (outil `clumsy`/`tc netem`) — critère : combos réalisables à 100 ms.

## 5. Profils

```java
public record CombatProfile(String key, boolean noCooldown, boolean legacyDamage, boolean swordBlocking,
                            boolean disableSweep, boolean disableOffhand, boolean legacyRegen,
                            boolean noPearlCooldown, boolean rodKnockback, boolean projectileKnockback,
                            int maximumNoDamageTicks, String knockbackProfile) {}
```

Presets fournis : `legacy-1.8` (tout activé), `modern` (rien), `hybrid` (pas de cooldown + KB 1.8 mais dégâts modernes).
Rush et Bedwars ont une option de config `combatProfile` (choix) et `knockbackProfile` (choix) → modifiables dans leur config.

## 6. Spikes à réaliser avant implémentation (tâches P0)

| Spike | Question | Critère de décision |
|---|---|---|
| **S1 — Block-hit** | Le composant `blocks_attacks` sur une épée (via l'API DataComponent de Paper) donne-t-il une pose de blocage acceptable et un état « bloque » détectable côté serveur (`Player#isBlocking` / `getActiveItem`) ? | Pose visuelle correcte en 1re et 3e personne, détection fiable. Sinon fallback : bouclier temporaire en main secondaire pendant le clic droit (masqué par modèle d'item). Documenter dans `docs/adr/ADR-007-sword-blocking.md`. |
| **S2 — Application des dégâts** | Comment imposer un montant final exact sans que l'armure moderne s'applique ? Options : (a) `EntityDamageEvent#setDamage(DamageModifier, double)` (API dépréciée mais présente ?) ; (b) type de dégâts custom `nexus:legacy_melee` fourni par datapack embarqué (tags `bypasses_armor`, `bypasses_effects`, `bypasses_enchantments`) + `victim.damage(final, DamageSource)` dans l'événement annulé ; (c) autre. | Montant final exact (± 0,01), attribution du tueur, son/animation de dégâts, knockback contrôlé, pas de double invulnérabilité. Choix documenté dans `ADR-008`. |
| **S3 — Knockback** | `EntityKnockbackByEntityEvent#setKnockback` couvre-t-il tous les cas (sprint, enchant, joueur en l'air) ? | Formule §4 reproduite à ± 2 % (mesure de déplacement). |

## 7. Recette (checklist manuelle)

- [ ] Spam clic : dégâts pleins à chaque coup ; aucune barre de recharge pénalisante.
- [ ] Épée diamant sur armure fer complète : dégâts identiques à la table de référence.
- [ ] Crit en sprint : ×1,5.
- [ ] W-tap / sprint reset : combo de 3+ coups possible face à un joueur qui ne réagit pas.
- [ ] Block-hit : dégâts reçus réduits, animation visible par les deux joueurs.
- [ ] Pas de balayage, pas de particule de sweep.
- [ ] Régénération lente type 1.8.
- [ ] Pomme d'or / enchantée : effets 1.8.
- [ ] Hors de l'instance (lobby) : combat moderne restauré.
