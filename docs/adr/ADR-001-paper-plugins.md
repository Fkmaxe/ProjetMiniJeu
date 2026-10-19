# ADR-001 — Plugins Paper (et non Minestom / Folia / mods)

- **Statut** : accepté
- **Date** : 2026-10-19

## Contexte
Le besoin exprimé est « un ensemble de plugins Minecraft » en 26.1, avec une forte exigence de performance et de facilité d'ajout de jeux.

## Options
1. **Paper** (+ API plugin) — écosystème mature, 26.1.2 stable, API Dialog, Display entities, DataComponents, Brigadier.
2. **Minestom** — serveur from scratch, très performant pour le mini-jeu, mais tout est à réimplémenter (physique, redstone, explosions, lits, IA des PNJ) et ce ne sont plus des « plugins ».
3. **Folia** — multithread par région ; utile pour de grands mondes partagés, peu pertinent pour des instances petites et nombreuses, et complexifie fortement le code (schedulers par région).
4. **Fabric/NeoForge serveur** — accès complet au code (26.1 non obfusqué) mais hors de la demande « plugins ».

## Décision
Paper 26.1.x. On compense l'absence de multithreading par **plusieurs JVM** (scaling horizontal) et **plusieurs instances par JVM** bien bornées.

## Conséquences
- Interdiction du NMS (ADR-006) pour rester portable entre builds Paper.
- La question Folia peut être rouverte si un jeu « grand monde » apparaît.
