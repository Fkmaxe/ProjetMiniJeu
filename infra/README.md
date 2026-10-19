# infra/

> **Dev local uniquement** pour `docker-compose.dev.yml`. La production se déploie sur **Pterodactyl** : voir `pterodactyl/` et `docs/17-deploiement-pterodactyl.md`. Les données de prod : `prod/docker-compose.data.yml`.

| Fichier | Rôle |
|---|---|
| `docker-compose.dev.yml` | réseau complet local : Postgres, Valkey, orchestrateur, Velocity, lobby-1, game-1, editor |
| `config/velocity.toml` | config proxy (modern forwarding, lobby de repli) |
| `config/forwarding.secret` | secret de dev — **à changer** hors dev |
| `config/paper/` | configs Paper de référence (P0-05) |
| `config/nexus/*.example` | exemples de `nexus.yml`, `nexus-proxy.yml`, `orchestrator.yml` |
| `pterodactyl/` | eggs + `nexus-boot.sh` (production) |
| `prod/` | services de données de production (Postgres, Valkey, sauvegardes) |
| `editor-imports/` | déposer ici un monde à importer dans l'éditeur (`/map import`) |

```bash
./gradlew build collectJars
docker compose -f infra/docker-compose.dev.yml up -d
docker compose -f infra/docker-compose.dev.yml logs -f orchestrator game-1
docker compose -f infra/docker-compose.dev.yml down        # -v pour effacer la BDD
```

Le premier démarrage de l'orchestrateur applique les migrations Flyway ; les serveurs Paper attendent que le schéma soit à jour.
Se donner le rôle owner en dev : `docker compose exec postgres psql -U nexus -c "INSERT INTO player_roles(player_id, role_id) SELECT '<uuid>', id FROM roles WHERE key='owner';"`
(le joueur doit s'être connecté une fois).
