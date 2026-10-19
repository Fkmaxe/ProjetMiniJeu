# ADR-005 — Système de rôles et permissions maison

- **Statut** : accepté
- **Date** : 2026-10-19

## Contexte
LuckPerms est le standard, mais la demande inclut un système de rôles intégré (UI, propagation, contexte de mode de jeu).

## Décision
Système maison : tables Postgres, résolution unique en Java pur (`PermissionResolver` dans `nexus-common`), propagation Redis, application par `PermissionAttachment` (Paper) et `PermissionProvider` (Velocity). Jokers développés contre les permissions connues ; le code Nexus interroge directement `PermissionService`.

## Conséquences
- Pas de dépendance tierce à configurer, une seule source de vérité, contexte `mode:<id>` natif.
- Plugins tiers : ils voient des permissions Bukkit standard ; un joker n'est vu par eux que si leur permission est enregistrée dans Bukkit.
- Si besoin plus tard : un pont LuckPerms pourrait remplacer l'implémentation derrière `PermissionService`.
