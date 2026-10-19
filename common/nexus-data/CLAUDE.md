# CLAUDE.md — `nexus-data` (accès BDD et Redis)

- Java pur, **aucune** dépendance Minecraft. Utilisé par `nexus-core`, `nexus-proxy`, `nexus-orchestrator`.
- Toutes les méthodes de repository sont **bloquantes** et documentées comme telles ; c'est l'appelant qui les exécute sur un virtual thread (`NexusScheduler.io()`). Ne pas renvoyer de `CompletableFuture` depuis un repo : ça masque le thread utilisé.
- SQL écrit à la main dans les repositories (`Pg<Name>Repository`), requêtes paramétrées uniquement (`PreparedStatement`), jamais de concaténation.
- Migrations : `src/main/resources/db/migration/V<n>__<snake_case>.sql`. **Jamais** modifier une migration existante. Seul l'orchestrateur exécute `flyway.migrate()` au démarrage ; les plugins font `flyway.validate()` et refusent de démarrer si le schéma est en retard.
- Pools Hikari : petits (`maximumPoolSize` 4–8 par serveur MC, 16 pour l'orchestrateur). Virtual threads + pool petit = file d'attente côté pool, c'est voulu.
- Redis : un `RedisClient` Lettuce partagé, une connexion pub/sub dédiée, les noms de clés viennent **exclusivement** de `RedisKeys` (miroir de `docs/04-messaging-protocol.md`).
- Tests d'intégration avec Testcontainers (`postgres:17`, `valkey/valkey:8`) dans `src/integrationTest`.
- Schéma documenté dans `docs/03-database.md` : le mettre à jour avec chaque migration.
