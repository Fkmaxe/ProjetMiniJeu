# ADR-007 — Block-hit à l'épée

- **Statut** : **à décider** (spike S1, tâche P0-06)

## Question
Comment reproduire le blocage à l'épée 1.8 sur clients 26.1 ?

## Options
1. Composant d'item `minecraft:blocks_attacks` sur les épées (DataComponent API de Paper), réduction de dégâts gérée par notre calculateur.
2. Bouclier temporaire en main secondaire pendant le clic droit (approche historique de certains plugins), apparence masquée.
3. Pas de block-hit (option de profil).

## Critères
Pose visuelle 1re/3e personne, détection serveur fiable de l'état « bloque », pas d'effet de bouclier parasite (son, désactivation par hache), coût nul hors combat.

## Décision
_À compléter après le spike._
