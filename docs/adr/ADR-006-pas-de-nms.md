# ADR-006 — Aucun NMS / internals

- **Statut** : accepté
- **Date** : 2026-10-19

## Décision
Uniquement l'API Paper, Adventure et les registres/datapacks officiels. Pas de `net.minecraft.*`, pas de `org.bukkit.craftbukkit.*`, pas de réflexion sur des champs internes, pas de ProtocolLib/PacketEvents en v1.

## Raisons
Paper livre de nouveaux builds 26.1.x fréquemment ; le NMS casse à chaque changement. L'API couvre en 26.1 tout ce dont on a besoin (Dialogs, Display entities, Mannequin, DataComponents, knockback events, registres).

## Exception
Uniquement par un nouvel ADR, avec code isolé dans un module `nexus-compat-*` derrière une interface et un fallback.
