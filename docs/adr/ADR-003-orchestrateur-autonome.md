# ADR-003 — Orchestrateur autonome (hors Minecraft)

- **Statut** : accepté
- **Date** : 2026-10-19

## Contexte
Il faut un « cerveau » unique pour les files, l'allocation des instances, le mode autonome et le scaling. Candidats : le proxy, un serveur lobby, un service dédié.

## Décision
Service Java 25 autonome `nexus-orchestrator`, avec élection de leader par verrou Redis (2 instances possibles).

## Raisons
- Le proxy peut être multiple (plusieurs Velocity) → la décision ne doit pas dépendre d'un proxy.
- Démarrer des conteneurs (provider docker) n'a rien à faire dans un processus Minecraft.
- Testable sans Minecraft (faux heartbeats).
- Point d'accroche naturel pour une future API REST / panneau web et pour les jobs (purge, leaderboards, expiration des rôles).

## Conséquences
- Un composant de plus à déployer (image Docker fournie).
- Si l'orchestrateur tombe, les parties en cours continuent ; seules les nouvelles affectations sont suspendues.
