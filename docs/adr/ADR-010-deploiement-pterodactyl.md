# ADR-010 — Déploiement sur Pterodactyl (mode pool)

- **Statut** : accepté
- **Date** : 2026-10-19

## Contexte
Les serveurs du développeur sont gérés avec Pterodactyl (Panel + Wings). Le réseau Nexus doit s'y déployer et l'orchestrateur doit pouvoir y ajuster le nombre de serveurs de jeu.

## Décision
- Tous les processus Nexus (proxy, lobbys, serveurs de jeu, editor, orchestrateur) sont des **serveurs Pterodactyl** créés depuis 3 eggs versionnés dans `infra/pterodactyl/eggs/`, avec un script de démarrage commun `nexus-boot.sh` qui installe la release du rôle.
- PostgreSQL et Valkey restent **hors du panel** (VM/LXC dédiée, `infra/prod/docker-compose.data.yml`).
- Scaling : provider `pterodactyl` en **mode pool** (serveurs pré-créés démarrés/arrêtés via l'API Client) ; mode `create` disponible mais secondaire.
- Le provider `docker` devient optionnel (dev/expérimentation).

## Raisons
- Pool : démarrage rapide, aucune installation pendant le pic, les Mounts (templates, tmpfs) ne sont pas configurables par l'API.
- Données hors panel : sauvegardes et cycle de vie indépendants, pas d'arrêt accidentel depuis le panel.
- Script de démarrage commun : une seule logique de mise à jour pour tous les rôles, testable hors panel.

## Conséquences
- `docs/17-deploiement-pterodactyl.md` est la référence d'exploitation et doit suivre chaque changement (règle 23 du `CLAUDE.md`).
- L'orchestrateur doit loguer exactement `Nexus orchestrator ready` (détection de démarrage de l'egg).
- Les serveurs annoncent leur adresse privée (`NEXUS_ADVERTISED_HOST`/`SERVER_PORT`).
