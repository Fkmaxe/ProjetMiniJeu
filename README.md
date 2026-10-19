# Nexus — réseau mini-jeux Minecraft (Paper 26.1)

Ensemble de plugins Paper + Velocity et d'un orchestrateur, reliés par PostgreSQL et Valkey (Redis),
pour faire tourner un serveur mini-jeux réparti sur plusieurs machines.

- **Parties automatiques** (files d'attente, compte à rebours, remplissage intelligent) et **à la demande** (admins, parties privées).
- **Ajout de jeux simplifié** : un module Gradle, une classe `GameDefinition`, des phases et des modules réutilisables.
- **Config admin in-game** générée automatiquement depuis la config typée de chaque jeu.
- **Stats & leaderboards** par jeu, variante et période (global, mensuel, hebdo).
- **Rôles / permissions**, **amis**, **party**.
- Jeux inclus : **Dropper**, **Rush**, **Bedwars**. PvP 1.8 optionnel par jeu.

## Démarrage rapide (dev)

Prérequis : JDK 25, Docker.

```bash
./gradlew build collectJars
docker compose -f infra/docker-compose.dev.yml up -d
# Se connecter en 26.1.x sur localhost:25565
```

## Déploiement (production)

Sur **Pterodactyl** : eggs et script de démarrage dans [`infra/pterodactyl/`](infra/pterodactyl/), procédure complète
dans [`docs/17-deploiement-pterodactyl.md`](docs/17-deploiement-pterodactyl.md). Postgres/Valkey hors panel : [`infra/prod/`](infra/prod/).

## Documentation

Tout est dans [`docs/`](docs/) — commencer par [`docs/00-vision-prd.md`](docs/00-vision-prd.md)
puis [`docs/01-architecture.md`](docs/01-architecture.md). Les développeurs (humains ou Claude Code)
lisent [`CLAUDE.md`](CLAUDE.md) en premier.

## Créer un nouveau mini-jeu

Voir [`docs/06-game-api.md`](docs/06-game-api.md) — section « Ajouter un jeu en 10 étapes »,
ou dans Claude Code : `/new-minigame <nom>`.

## Travailler avec Claude Code

Toujours en **mode plan** : passer en mode plan (Shift+Tab, ou préfixer la demande par `/plan`) puis lancer `/implement-task P0-01` → relire et approuver le plan (format imposé par `CLAUDE.md` §8)
→ Claude exécute étape par étape, **un commit par étape**, avec ton identité Git et sans aucune ligne d'attribution.
