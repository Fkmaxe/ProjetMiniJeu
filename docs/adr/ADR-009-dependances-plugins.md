# ADR-009 — Embarquer les dépendances (Shadow) ou les charger (loader Paper)

- **Statut** : **à décider** (tâche P0-03)

## Question
Comment fournir HikariCP, Flyway, le driver Postgres, Lettuce et Caffeine à `nexus-core` / `nexus-lobby` ?

## Options
1. **Shadow + relocalisation** `fr.nexus.libs.*` : jar autonome, aucune requête réseau au démarrage, jars plus gros.
2. **Loader de bibliothèques Paper** (`PluginLoader` + `MavenLibraryResolver` dans `paper-plugin.yml`) : jars légers, téléchargement au premier démarrage (nécessite l'accès à Maven Central depuis les serveurs Pterodactyl).

## Critères
Démarrage hors-ligne possible sur Pterodactyl, absence de conflit avec d'autres plugins, taille des archives de release, simplicité.

## Décision
_À compléter._ (Recommandation initiale : Shadow, pour ne dépendre d'aucun accès réseau au démarrage des serveurs.)
