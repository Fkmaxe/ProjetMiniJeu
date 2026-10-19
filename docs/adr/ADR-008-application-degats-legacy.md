# ADR-008 — Application des dégâts « 1.8 »

- **Statut** : **à décider** (spike S2, tâche P0-07)

## Question
Comment imposer le montant final calculé par `LegacyDamageCalculator` sans que la réduction d'armure/effets moderne s'applique en plus ?

## Options
1. `EntityDamageEvent#setDamage(DamageModifier, double)` (API dépréciée — vérifier son comportement en 26.1).
2. Type de dégâts custom via datapack embarqué (`nexus:legacy_melee`, tags `bypasses_armor`, `bypasses_effects`, `bypasses_enchantments`) : annuler l'événement d'origine et appliquer `victim.damage(final, DamageSource)` avec l'attaquant comme cause.
3. Neutraliser l'armure (attributs à 0) et tout recalculer — rejeté a priori : la barre d'armure du client serait fausse.

## Critères
Montant exact, attribution du kill, animation/son de dégâts, knockback contrôlé par notre formule, invulnérabilité cohérente, compatibilité avec les autres sources de dégâts (flèches, TNT, chute).

## Décision
_À compléter après le spike._
