# infra/pterodactyl/

| Fichier | Rôle |
|---|---|
| `eggs/egg-nexus-paper.json` | egg des serveurs Paper (rôles lobby / game / editor) |
| `eggs/egg-nexus-velocity.json` | egg du proxy |
| `eggs/egg-nexus-orchestrator.json` | egg de l'orchestrateur |
| `nexus-boot.sh` | script de démarrage commun (synchronisation de la release, secrets, JVM) — publié aussi avec chaque release |

Procédure complète, variables, montages, scaling et mise à jour : **`docs/17-deploiement-pterodactyl.md`**.

Règle : modifier un egg = le réexporter/réimporter dans le panel **et** mettre à jour la doc 17 dans le même commit.
Test local du script : `docs/17` n'en dépend pas, mais `nexus-boot.sh` peut être exécuté hors panel en définissant
`NEXUS_ROLE`, `NEXUS_RELEASE_URL`, `NEXUS_VERSION`, `VELOCITY_SECRET`, `SERVER_MEMORY`.
