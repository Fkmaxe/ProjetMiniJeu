# 16 — Workflow Git & commits

## 1. Identité des commits — règle absolue

Les commits sont faits **avec le compte Git du développeur** configuré sur sa machine (`user.name` / `user.email`).
Aucun commit, message, branche ni description de PR ne doit mentionner un outil d'assistance.

Pour Claude Code (et tout autre outil) :
1. **Ne jamais modifier la config Git** : pas de `git config` (local ou global), pas de `-c user.name=…`, pas de `--author`, pas de variables `GIT_AUTHOR_*` / `GIT_COMMITTER_*`.
2. Avant le premier commit d'une session : `git config user.email` doit renvoyer une valeur. Si elle est vide → **s'arrêter et demander** au développeur de la configurer lui-même.
3. **Aucune ligne d'attribution** dans les messages : pas de `Co-Authored-By`, pas de `Claude-Session`, pas de « Generated with … », pas de lien de session, pas d'emoji robot. Même chose dans les descriptions de PR et les notes de version.
4. Ne pas mentionner l'outil dans les messages : un commit qui modifie `CLAUDE.md` ou `.claude/` se décrit fonctionnellement (`docs: règles de contribution`, `chore: commandes de développement`).
5. `.claude/settings.json` (versionné) désactive l'attribution automatique de Claude Code ; le hook `commit-msg` (§5) rejette tout message qui en contiendrait malgré tout.
6. Ne jamais contourner les hooks (`--no-verify` interdit).

## 2. Committer à chaque étape

« Étape » = une unité **cohérente, compilable et testée**. Les étapes sont **fixées dans le plan approuvé** (mode plan,
section « Étapes = commits » du format du `CLAUDE.md` §8) : chaque étape du plan donne un commit, avec le message prévu.
Si l'exécution impose un découpage différent, on le signale avant de committer. Concrètement, pendant une tâche de roadmap :

| Moment | Commit |
|---|---|
| Plan validé qui change l'API, le schéma ou le protocole | `docs(...)` : mise à jour de la doc/contrat seule |
| Nouvelle migration Flyway | commit dédié `feat(data): migration V<n> ...` |
| Chaque sous-partie qui passe `./gradlew build` (ex. calculateur + tests, puis listeners, puis module) | `feat` / `fix` / `refactor` / `test` selon le contenu |
| Formatage ou refactoring hors périmètre | commit séparé `style` / `refactor` |
| Fin de tâche | dernier commit qui **coche la tâche dans la roadmap** et met à jour la doc si besoin |

Règles :
- Jamais de commit rouge : `./gradlew build` (ou au minimum `./gradlew :<module>:check`) vert avant chaque commit ; `./gradlew check` complet avant le commit de fin de tâche.
- Un commit = **une intention** (ne pas mélanger feature + refactoring + formatage).
- Taille visée : < 400 lignes modifiées hors fichiers générés/ressources ; au-delà, découper.
- Ne committer que des fichiers liés à l'étape (`git add <fichiers>` explicite, jamais `git add -A` aveugle) ; vérifier `git status` et `git diff --staged` avant.
- Ne jamais committer : secrets (`forwarding.secret` hors dev, mots de passe), `build/`, `.gradle/`, mondes, `*.log`, `.claude/settings.local.json`.

## 3. Format des messages (Conventional Commits)

```
<type>(<scope>): <résumé à l'impératif, ≤ 72 caractères, sans point final>

<corps facultatif : le POURQUOI, ce qui change pour l'utilisateur/le dev, lignes ≤ 100 caractères>

Refs: <ID de tâche>
```

- **types** : `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`, `style`, `revert`.
- **scopes** : `common`, `data`, `api`, `core`, `combat`, `lobby`, `proxy`, `orchestrator`, `dropper`, `rush`, `bedwars`, `infra`, `deploy` (Pterodactyl : eggs, `nexus-boot.sh`, doc 17), `docs`, `build`.
- Langue : **français** (cohérent avec la doc), résumé à l'impératif (« ajoute », « corrige »).
- Le pied `Refs: P3-06` est **obligatoire** pour tout commit lié à une tâche de roadmap (vérifié par le hook, sauf `docs`, `style`, `chore`).
- Changement cassant de `nexus-api` ou du protocole : `feat(api)!:` + paragraphe `BREAKING CHANGE:`.

Exemples :
```
feat(combat): calcule les dégâts 1.8 des armes et armures

Reproduit l'ordre de calcul 1.8 (force multiplicative, crit, tranchant,
armure à 4 % par point) dans un calculateur pur, couvert par une table
de référence.

Refs: P7-01
```
```
fix(core): libère la réservation quand le transfert échoue

Refs: P2-06
```

## 4. Branches & PR

- `main` protégée : toujours verte, déployable.
- Une branche par tâche : `feat/P3-06-instance-manager`, `fix/P2-06-reservation-leak`.
- Fusion par PR (squash interdit pour garder les étapes ; **rebase-merge** ou merge commit selon la préférence du dépôt), CI verte obligatoire.
- Description de PR : objectif, ID de tâche, comment tester, captures si UI ; **sans** ligne d'attribution.
- Pas de `push --force` sur une branche partagée ; `--force-with-lease` seulement sur sa propre branche.

## 5. Hooks Git versionnés (`.githooks/`)

| Hook | Vérifie |
|---|---|
| `commit-msg` | format Conventional Commits ; résumé ≤ 72 car. ; `Refs:` présent pour feat/fix/refactor/perf/test ; **rejet** de toute ligne d'attribution (Co-Authored-By d'un assistant, `Claude-Session`, « Generated with », adresse `noreply@anthropic.com`, mention « Claude Code ») |
| `pre-commit` | `./gradlew spotlessCheck --offline -q` (rapide) ; refuse les fichiers interdits (§2) et les secrets évidents |

Installation : `./gradlew installGitHooks` (copie `.githooks/*` dans `.git/hooks/`, rendue automatique par `build` — tâche P0-12). Aucune modification de config Git nécessaire.
