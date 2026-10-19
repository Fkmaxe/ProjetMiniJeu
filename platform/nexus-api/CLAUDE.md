# CLAUDE.md — `nexus-api` (API publique des jeux)

Ce module est le **contrat** entre la plateforme et les mini-jeux. Le modifier a un impact sur tous les jeux.

- Ne contient **que** des interfaces, records, enums, annotations et quelques implémentations par défaut sans état (builders, modules génériques sans dépendance au core).
- Aucune dépendance vers `nexus-core`, `nexus-data`, Redis, JDBC. Dépendances autorisées : `paper-api` (compileOnly), `nexus-common`.
- **Compatibilité** : on ajoute, on ne casse pas. Une suppression/renommage passe par `@Deprecated(forRemoval = true, since = "x.y")` pendant au moins une version mineure.
- Toute nouvelle méthode publique a une Javadoc (rôle, thread autorisé, nullabilité).
- Indiquer le **thread** dans la Javadoc : `@MainThread` ou `@AnyThread` (annotations maison dans `fr.nexus.api.annotation`).
- Avant d'ajouter une méthode « pratique », vérifier qu'elle ne peut pas être un **module** (`GameModule`) réutilisable.
- Mettre à jour `docs/06-game-api.md` dans le même commit.
- Version de l'API dans `gradle.properties` (`nexusApiVersion`), vérifiée au chargement d'un jeu (`api-version` majeure identique requise).
