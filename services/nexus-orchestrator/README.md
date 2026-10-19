# nexus-orchestrator (service autonome)

Application Java 25 : migrations Flyway, registre serveurs/instances, files & matchmaking, mode autonome,
création à la demande, **scaling Pterodactyl**, jobs (leaderboards, purges, expiration des rôles), métriques Prometheus.

Doit écrire exactement la ligne `Nexus orchestrator ready` dans ses logs quand il est prêt (détection de démarrage de l'egg Pterodactyl).

Docs : `docs/05-orchestration-matchmaking.md`, `17-deploiement-pterodactyl.md` §6, ADR-003.
Config : `infra/config/nexus/orchestrator.yml.example`. Tâches : P2-04..07, P10-03, P10-05, P11-01.
