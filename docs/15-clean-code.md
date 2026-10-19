# 15 — Clean code

Complète `13-coding-standards.md` (conventions techniques). Ici : **les principes de conception** et **les outils qui les font respecter**.
Une règle qui n'est vérifiée par aucun outil est vérifiée par l'agent `clean-code-reviewer` avant chaque commit de fin de tâche.

## 1. Principes

### Lisibilité d'abord
- Le code se lit comme la spec : noms **métier** issus de la doc et du glossaire (`Instance`, `Preset`, `Marqueur` → `MarkerType`), jamais `data`, `info`, `manager2`, `tmp`, `handle()`.
- Noms : classes = noms (`BedDestroyedEvent`), méthodes = verbes (`assignTeam`), booléens = questions (`isJoinable`, `hasBed`), collections au pluriel. Pas d'abréviations sauf usuelles (`id`, `uuid`, `mspt`).
- Code et identifiants en **anglais** ; textes joueurs via i18n ; commentaires et Javadoc en **français ou anglais, mais cohérents par module** (défaut : anglais dans le code, français dans `docs/`).

### Fonctions
- **Une fonction = une chose**, à un seul niveau d'abstraction. Cible ≤ 25 lignes, maximum toléré 40 (vérifié par Checkstyle).
- ≤ 3 paramètres ; au-delà → un record de paramètres (`AttackInput`).
- **Retours anticipés** (guard clauses) plutôt qu'imbrications ; profondeur d'imbrication ≤ 3.
- Pas de paramètre booléen qui change le comportement (`build(true)`) → deux méthodes ou un enum.
- Pas d'effets de bord cachés : une méthode `get`/`is`/`compute` ne modifie rien.
- Command/Query separation : une méthode fait une action **ou** renvoie une information.

### Classes & modules (SOLID appliqué à Nexus)
| Principe | Traduction concrète |
|---|---|
| **S** — responsabilité unique | un listener ne contient pas de règles de jeu : il traduit l'événement Bukkit et délègue à une classe de `logic/` |
| **O** — ouvert/fermé | nouveau comportement de jeu = nouveau **module** ou nouvelle **phase**, pas un `if (mode == BEDWARS)` dans le core |
| **L** — substitution | toute implémentation de `GameModule`, `Matchmaker`, `WorldProvider`, `ScalingProvider` respecte le contrat Javadoc (préconditions, thread) |
| **I** — interfaces fines | `StatsRecorder`, `GameUi`, `ScopedEvents` séparés plutôt qu'un `GameContext` fourre-tout côté implémentation |
| **D** — inversion | le core dépend d'interfaces (`PlayerRepository`, `MessageBus`) injectées par constructeur ; aucun `new PgPlayerRepository()` hors du `Bootstrap` |

- **Composition plutôt qu'héritage** : pas de hiérarchie de plus de 2 niveaux hors API Bukkit ; préférer interfaces + délégation.
- Pas de classes « Utils » fourre-tout : un utilitaire appartient à un concept (`Durations`, `TeamColors`).
- Encapsulation : champs `private final` par défaut, pas de getters/setters générés en masse ; exposer des **comportements** (`team.eliminate()`) plutôt que des données (`team.setAlive(false)`).
- Loi de Déméter : éviter `ctx.players().get(id).team().bed().location()` dans la logique → méthode dédiée (`ctx.teams().bedOf(player)`).

### Duplication & valeurs
- **DRY raisonnable** : à la 3ᵉ répétition, factoriser ; un comportement présent dans 2 jeux → module de l'API.
- **Aucun nombre magique** : constante nommée, champ de config (jeu) ou entrée de `LegacyTables` (combat). Seules exceptions : 0, 1, -1, et les ticks/secondes via `Ticks.seconds(5)`.
- Aucune chaîne magique : clés i18n, clés Redis (`RedisKeys`), permissions (`Permissions`), types de messages (`MessageType`) centralisés.

### Erreurs
- Erreurs attendues → `Result<T, E>` ; inattendues → exception avec contexte. Jamais `return null` pour signaler une erreur.
- On ne capture que ce qu'on sait traiter ; `catch (Exception)` uniquement aux frontières (dispatcher d'événements, handler de message) et toujours logué.

### Commentaires
- Le code dit **comment**, le commentaire dit **pourquoi** (règle 1.8 citée, contrainte de performance, lien vers la spec : `// cf. docs/games/bedwars.md §4`).
- Pas de code commenté, pas de `TODO` sans ID de tâche (`// TODO(P9-02): pièges multiples`).
- Javadoc obligatoire sur toute API publique (`nexus-api`, interfaces de service) : rôle, thread, nullabilité, exceptions.

### Tests
- Structure **Arrange / Act / Assert**, un comportement par test, nommage `method_condition_expected`.
- Pas de logique dans les tests (pas de boucles/if) ; jeux de données via `@ParameterizedTest`.
- Un bug corrigé = un test qui le reproduit, dans le même commit.
- Tests rapides (< 100 ms) pour la logique pure ; les tests lents vont dans `integrationTest`.

### Règle du boy-scout
Laisser chaque fichier touché un peu plus propre — **dans le périmètre de la tâche** : un refactoring hors périmètre fait l'objet d'un commit séparé `refactor(...)`.

## 2. Outils qui imposent ces règles (installés en P0-11)

| Outil | Rôle | Échoue le build si |
|---|---|---|
| **Spotless** + `palantir-java-format` | formatage unique, imports triés, fin de ligne | `spotlessCheck` échoue (corriger avec `./gradlew spotlessApply`) |
| **Error Prone** + **NullAway** | bugs courants, nullabilité JSpecify | toute erreur ; warnings Error Prone = erreurs dans `common/` et `platform/nexus-api` |
| **Checkstyle** (config `config/checkstyle/nexus.xml`) | longueur de méthode (40), de fichier (400), nb de paramètres (3, records exclus), profondeur d'imbrication (3), nombres magiques, imports étoile, nommage | toute violation |
| **ArchUnit** (tests `ArchitectureTest` par module) | architecture : couches, pas d'import core dans les jeux, pas de `Bukkit.*` dans `logic/` et `calc/`, pas de JDBC hors `nexus-data`, nommage des suffixes (`*Repository` dans `repo`), pas de cycles entre packages | test rouge |
| **JaCoCo** | couverture | < 80 % sur `calc/`, `logic/`, `Matchmaker`, `PermissionResolver` |
| `checkDependencyRules` | dépendances entre modules Gradle | règle violée |
| `checkI18n` | clés FR ↔ EN | clé manquante |

Commande de vérification complète avant un commit de fin d'étape : `./gradlew check` (= formatage, analyse statique, tests, architecture, couverture, i18n).

## 3. Checklist de relecture (agent `clean-code-reviewer`)

- [ ] Les noms correspondent au vocabulaire de la spec / du glossaire.
- [ ] Aucune méthode > 40 lignes, aucune classe > 400 lignes, imbrication ≤ 3.
- [ ] Aucune logique de règle dans un listener / une commande / un menu.
- [ ] Aucun nombre ni chaîne magique.
- [ ] Pas de duplication évitable (≥ 3 occurrences ou 2 jeux).
- [ ] Erreurs gérées selon §1 ; aucun `catch` silencieux.
- [ ] Javadoc présente et exacte sur les API publiques.
- [ ] Tests AAA, un comportement par test, cas limites de la spec couverts.
- [ ] Pas de code mort, pas de code commenté, `TODO` avec ID.
