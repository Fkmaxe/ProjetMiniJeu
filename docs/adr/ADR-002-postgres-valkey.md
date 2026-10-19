# ADR-002 — PostgreSQL + Valkey (Redis)

- **Statut** : accepté
- **Date** : 2026-10-19

## Décision
- **PostgreSQL 17** pour le durable : transactions pour les rapports de match, JSONB pour configs/maps/préférences, index partiels (preset actif), `ON CONFLICT` pour les upserts de stats.
- **Valkey 8** (fork open source compatible Redis) pour le temps réel : pub/sub, TTL (présence, réservations, invitations), ZSET (leaderboards). Client **Lettuce** (async, compatible virtual threads).

## Alternatives rejetées
- MySQL/MariaDB : fonctionnel, mais JSONB/index partiels/upserts conditionnels plus pratiques en Postgres.
- MongoDB : pas de transactions simples multi-collections pour les stats, pas de schéma.
- Bus dédié (NATS, RabbitMQ) : un composant de plus ; Redis couvre déjà état + messages à cette échelle. Interface `MessageBus` pour pouvoir changer.

## Conséquences
- Redis n'est **pas** une source de vérité durable : tout ce qui s'y trouve doit être reconstructible (`server.resync`).
