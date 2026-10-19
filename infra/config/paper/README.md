Configs de référence des serveurs Paper Nexus (cf. docs/12-performance.md §4) :

| Fichier | Usage |
|---|---|
| `server.properties` | valeurs de référence ; sous Pterodactyl, l'egg gère `server-ip`/`server-port` |
| `paper-global.yml` | **extrait** à fusionner dans le fichier généré par Paper (secret Velocity injecté par `nexus-boot.sh`) |
| `paper-world-defaults.yml` | **extrait** à fusionner |

Ces fichiers sont inclus dans les archives de release (`config/`) et copiés seulement s'ils sont absents du serveur.
Vérification des clés contre la doc Paper 26.1 : tâche P0-05.
