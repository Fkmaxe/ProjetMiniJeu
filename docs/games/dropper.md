# Dropper (`dropper`)

> Statut : validé (sous réserve des retours) · Version de spec : 1 · PvP : non

## 1. Pitch
Course de chute libre : les joueurs se jettent dans un puits truffé d'obstacles et doivent atterrir dans la zone d'arrivée
(eau, slime, zone dédiée). Plusieurs niveaux enchaînés, le plus rapide gagne. Premier jeu implémenté : il valide toute la plateforme.

## 2. Variantes
| Variante | Équipes × taille | Min joueurs | File auto |
|---|---|---|---|
| `solo` | 12 × 1 | 2 | oui |

## 3. Déroulé
1. **Waiting** : spawn d'attente avec panneau des records de la map (top 5, hologramme), hotbar : « Quitter », « Records ».
2. **Countdown** : standard (cf. `05` §3).
3. **Race** (phase principale, limite `timeLimitSeconds`) : tous les joueurs démarrent au niveau 1 simultanément ; chacun progresse à son rythme.
4. **End** : podium, récap (temps de chacun, nombre d'échecs), records battus mis en avant.

Fin de la course : quand **tous** les joueurs ont fini, ou à la limite de temps, ou si `endWhenFirstFinishes` et un joueur a fini + `graceSecondsAfterFirst` écoulées.

## 4. Règles
- Atterrir dans la région `level_finish[i]` → niveau suivant (téléportation au `level_start[i+1]`) ; dernier niveau → arrivée, temps figé.
- Toucher le sol hors de la zone d'arrivée, ou passer sous `void_y` → **échec** : retour au début du niveau (ou du parcours si `failAction = RESTART_ALL`), compteur d'échecs +1.
- Détection d'atterrissage : `PlayerMoveEvent` (changement de bloc) + vérification « au sol » (`isOnGround` côté serveur **non fiable** → on teste le bloc sous les pieds et la vitesse verticale ≈ 0 sur 2 ticks consécutifs) ; l'eau d'arrivée est détectée par la région.
- Classement : joueurs arrivés par temps croissant, puis non-arrivés par niveau atteint décroissant, puis par temps d'atteinte de ce niveau.
- Aucun dégât, aucune faim, aucune construction, collisions entre joueurs **désactivées** (équipe scoreboard avec `collisionRule NEVER`), joueurs semi-transparents entre eux (`canSeeFriendlyInvisibles`) selon option.
- Saut de niveau : interdit ; `skipLevelAfterFails` (option) permet de passer un niveau après N échecs avec pénalité de temps.
- Déconnexion : rejoin 60 s, chrono continue ; au-delà, outcome `LEFT`.

Résultat : `GameResult.ranking(...)` → 1er = WIN, autres = LOSS. Une partie solo (1 joueur forcé) est non classée.

## 5. Config (`DropperConfig`)
| Catégorie | Option | Type | Bornes | Défaut | Hôte ? | Description |
|---|---|---|---|---|---|---|
| Général | `levels` | int | 1–10 | 5 | oui | Nombre de niveaux joués (tirés parmi ceux de la map dans l'ordre, ou aléatoirement si `randomLevelOrder`) |
| Général | `randomLevelOrder` | bool | — | false | oui | Ordre aléatoire des niveaux |
| Général | `timeLimitSeconds` | int | 60–1800 (pas 30) | 600 | oui | Durée maximale |
| Fin | `endWhenFirstFinishes` | bool | — | false | oui | Terminer peu après la première arrivée |
| Fin | `graceSecondsAfterFirst` | int | 5–120 | 30 | oui | Délai laissé aux autres |
| Règles | `failAction` | enum `RESTART_LEVEL / RESTART_ALL` | — | RESTART_LEVEL | oui | Effet d'un échec |
| Règles | `skipLevelAfterFails` | int | 0–50 | 0 | oui | 0 = jamais |
| Règles | `skipPenaltySeconds` | int | 0–120 | 15 | oui | Pénalité ajoutée au temps |
| Règles | `playerVisibility` | enum `VISIBLE / GHOST / HIDDEN` | — | GHOST | oui | Visibilité des autres coureurs |
| Visuels | `trails` | bool | — | true | non | Traînée de particules |
| Visuels | `finishFireworks` | bool | — | true | non | Feu d'artifice à l'arrivée |

Presets : `classic` (défauts), `sprint` (3 niveaux, 180 s, fin à la première arrivée), `marathon` (10 niveaux, 1 800 s).

## 6. Schéma de map
| Élément | Type | Cardinalité | Rôle |
|---|---|---|---|
| `waiting_spawn` | point + rotation | 1 | attente |
| `level_start` | point + rotation, indexé | ≥ 1 | départ de chaque niveau |
| `level_finish` | région, indexée | = nb de `level_start` | zone d'arrivée de chaque niveau |
| `podium` | point, indexé | 0–3 | positions du podium de fin |
| `records_board` | point | 0–1 | position de l'hologramme des records |
| `void_y` | méta int | — | Y sous lequel c'est un échec |
| `level_names` | méta liste de textes | — | noms affichés des niveaux |

Validation : autant de `level_finish` que de `level_start` ; `levels` de chaque preset actif ≤ nb de niveaux de la map (avertissement sinon).

## 7. Modules
`NoHungerModule`, `DamageRulesModule.disableAll()`, `BuildRulesModule.denyAll()`, `SpectatorModule` (pour les arrivés qui veulent regarder), `CheckpointModule` (niveaux).

## 8. Stats & records
| Clé | Agrégation | Leaderboard | Description |
|---|---|---|---|
| `levels_completed` | SUM | oui | Niveaux terminés |
| `fails` | SUM | non | Échecs |
| `finishes` | SUM | oui | Parcours terminés |
| record `best_time:<mapKey>` | MIN | oui (par map) | Meilleur temps complet sur la map |
| record `best_level:<mapKey>:<index>` | MIN | non | Meilleur temps par niveau (affiché en jeu : « −0,4 s sur ton record ») |

## 9. UI
- Sidebar : niveau `3/5`, temps, échecs, position actuelle (calculée 1×/s), record perso.
- Action bar : chrono au 1/10 s pendant la chute (mise à jour toutes les 2 ticks pour le joueur concerné uniquement).
- Titre à chaque niveau : nom du niveau.
- Fin : Dialog récap (classement, temps, records battus).

## 10. Visuels
Traînées `TRAIL`/`DUST` colorées par joueur ; anneau animé (`BlockDisplay` interpolé) autour de la zone d'arrivée ; flash + son à l'atterrissage réussi ; podium avec `Mannequin`.

## 11. Cas limites
- Joueur qui atterrit pile en bordure de région : la région est étendue de 0,3 bloc vers le bas.
- Ender pearls/elytres : retirés (inventaire vidé, `BuildRules`/`ItemRules`).
- Lag : un échec n'est validé qu'après 2 ticks au sol (anti faux positif).
- Map avec moins de niveaux que `levels` : on joue tous les niveaux de la map.

## 12. Recette
- [ ] 2 joueurs, 3 niveaux : progression, échec, arrivée, classement et stats en BDD.
- [ ] Record battu → `player_records` mis à jour, annonce, hologramme rafraîchi.
- [ ] Déconnexion/reconnexion pendant la course.
- [ ] Fin au temps limite avec classement par niveau atteint.
