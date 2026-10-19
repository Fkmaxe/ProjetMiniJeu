# Rush (`rush`) — façon Funcraft

> Statut : **à valider** (cf. Q1 dans `00-vision-prd.md`) · Version de spec : 1 · PvP : legacy 1.8

## 1. Pitch
Deux équipes, deux bases face à face reliées par la map, un lit à défendre. On récolte bronze, fer et or générés
dans sa base, on achète blocs et équipement au marchand, on **rushe** la base adverse pour casser son lit puis
éliminer ses joueurs. Plus court, plus dense et plus « PvP » que le Bedwars : pas d'îles dans le vide, pas
d'améliorations d'équipe, pas de diamants/émeraudes.

## 2. Variantes
| Variante | Équipes × taille | Min joueurs | File auto |
|---|---|---|---|
| `2x4` | 2 × 4 | 4 | oui |
| `4x2` | 4 × 2 | 4 | non (à la demande / événements) |
| `2x2`, `2x1` | à ajouter par migration si besoin | | |

## 3. Déroulé
1. **Waiting** : salle d'attente ; choix d'équipe via PNJ colorés (si `allowTeamChoice`) en respectant l'équilibre et les parties.
2. **Countdown** : standard.
3. **Prep** (option, `prepSeconds`, défaut 0) : barrières au milieu, chacun s'équipe.
4. **Battle** : phase principale. À `suddenDeathAfterMinutes` → **SuddenDeath** : tous les lits restants sont détruits (animation + annonce), régénération désactivée (option).
5. **End** : victoire de la dernière équipe en vie ; si `maxDurationMinutes` atteinte → égalité ou victoire au plus de joueurs vivants (option `timeoutRule`).

## 4. Règles
- **Lit** : tant que le lit de l'équipe existe, un joueur mort réapparaît après `respawnSeconds` à son spawn. Lit détruit → la mort suivante est **définitive** (final kill). Un joueur ne peut pas casser le lit de son équipe.
- **Ressources** : items-monnaies (bronze = `BRICK`, fer = `IRON_INGOT`, or = `GOLD_INGOT`, matériaux configurables) apparaissent sur les générateurs de chaque base ; ramassage normal ; `shareResources` donne une copie aux coéquipiers à 2 blocs.
- **Construction** : blocs posables uniquement s'ils viennent du shop (ou de la liste `placeableBlocks`) ; on ne casse **que les blocs posés en partie** (+ lit adverse) ; hauteur limitée (`buildHeightMin/Max` = méta de map) ; zones protégées : spawns, générateurs, marchands (régions de la map).
- **TNT** : allumage instantané à la pose (option), dégâts aux joueurs, ne détruit **que** les blocs posés et jamais les blocs protégés ; les lits résistent aux explosions (option).
- **Mort** : drop des **monnaies uniquement** au tueur (les armures/épées restent ; outils gardés selon `keepToolsOnDeath`) ; mort dans le vide ou par chute attribuée au dernier attaquant (< 10 s).
- **Déconnexion** : rejoin 120 s si le lit est debout ; si le lit tombe pendant l'absence, le joueur revient spectateur… sauf s'il est encore « en vie » (il n'était pas mort) → il revient normalement mais sa prochaine mort est finale.
- **Victoire** : dernière équipe avec au moins un joueur vivant (`TeamEliminatedEvent` déclenche la vérification).

## 5. Config (`RushConfig`)
| Catégorie | Option | Type | Bornes | Défaut | Hôte ? |
|---|---|---|---|---|---|
| Général | `maxDurationMinutes` | int | 5–60 | 30 | oui |
| Général | `suddenDeathAfterMinutes` | int | 0–60 (0 = jamais) | 20 | oui |
| Général | `timeoutRule` | enum `DRAW / MOST_ALIVE / MOST_BEDS` | — | MOST_ALIVE | oui |
| Général | `prepSeconds` | int | 0–120 | 0 | oui |
| Équipes | `allowTeamChoice` | bool | — | true | oui |
| Respawn | `respawnSeconds` | int | 0–15 | 5 | oui |
| Respawn | `keepToolsOnDeath` | bool | — | true | oui |
| Respawn | `spawnProtectionSeconds` | int | 0–10 | 3 | oui |
| Ressources | `generators` | liste de `GeneratorDef(resource, item, intervalTicks, amount, maxOnGround)` | 1–6 | bronze 20 t ×1 (max 64), fer 160 t (max 16), or 400 t (max 8) | non |
| Ressources | `shareResources` | bool | — | false | oui |
| Boutique | `shop` | `ShopCatalog` (catégories → offres : item, quantité, prix (ressource, nb), enchantements, `persistent`) | — | catalogue §5.1 | non |
| Construction | `placeableBlocks` | liste d'items | — | grès coupé, laine d'équipe, verre trempé, planches, échelles | non |
| Construction | `tntInstant` | bool | — | true | oui |
| Construction | `bedsBlastProof` | bool | — | true | oui |
| Combat | `combatProfile` | choix parmi les profils | — | `legacy-1.8` | oui |
| Combat | `knockbackProfile` | choix | — | `1.8-vanilla` | oui |
| Combat | `friendlyFire` | bool | — | false | oui |
| Visuels | `bedShatter`, `killEffects`, `introCamera` | bool | — | true, true, false | non |

### 5.1 Catalogue par défaut (éditable dans l'UI : menu de liste)
| Catégorie | Offre | Prix |
|---|---|---|
| Blocs | 16 grès | 4 bronze |
| Blocs | 16 laine d'équipe | 4 bronze |
| Blocs | 4 verre trempé | 12 bronze |
| Blocs | 3 échelles | 2 bronze |
| Armes | Épée en pierre | 1 fer |
| Armes | Épée en fer | 3 or |
| Armes | Arc + 8 flèches | 4 or |
| Armure | Plastron/jambières/bottes en cuir (teint équipe) | 2 bronze pièce |
| Armure | Plastron en mailles | 2 fer |
| Armure | Plastron en fer | 4 or |
| Outils | Pioche en bois → pierre → fer (`persistent`, paliers) | 4 bronze / 2 fer / 2 or |
| Spécial | TNT | 2 or |
| Spécial | Perle de l'Ender | 6 or |
| Spécial | Pomme d'or | 1 or |
| Nourriture | 4 steaks | 4 bronze |

## 6. Schéma de map
| Élément | Type | Cardinalité | Rôle |
|---|---|---|---|
| `waiting_spawn` | point | 1 | attente |
| `team_spawn` | point + rotation, `perTeam` | = nb équipes | réapparition |
| `team_bed` | bloc, `perTeam` | = nb équipes | lit (pied ou tête, l'autre moitié déduite) |
| `generator` | point, `perTeam`, attribut `resource` (bronze/fer/or) | ≥ 1 par ressource et par équipe | générateurs |
| `shop_npc` | point + rotation, `perTeam` | ≥ 1 par équipe | marchand |
| `team_chooser` | point + rotation, `perTeam` | 0 ou = nb équipes | PNJ de choix d'équipe en attente |
| `protected` | région | ≥ 0 | zones non constructibles |
| `build_area` | région | 0–1 | si défini, construction uniquement dedans |
| `void_y`, `build_min_y`, `build_max_y` | méta int | — | limites |

Validation : pour chaque variante compatible, nb de `team_*` = nb d'équipes ; chaque équipe a au moins un générateur de chaque ressource définie dans le preset actif ; le lit est un vrai bloc de lit.

## 7. Modules
`LegacyCombatModule`, `BedModule`, `GeneratorModule`, `ShopModule`, `RespawnModule` (condition : lit debout), `VoidDeathModule`, `BuildRulesModule`, `TeamChatModule`, `KitModule` (kit de départ : épée en bois, armure en cuir d'équipe), `SpectatorModule`, `TimeLimitModule` + `SuddenDeathPhase`.

## 8. Stats
| Clé | Agrégation | Leaderboard |
|---|---|---|
| `kills` | SUM | oui |
| `final_kills` | SUM | oui |
| `deaths` | SUM | non |
| `beds_broken` | SUM | oui |
| `resources_collected` | SUM | non |
| (auto) `wins`, `losses`, `games_played`, `win_streak`, `best_win_streak` | | oui (wins) |

## 9. UI
- Sidebar : temps restant avant mort subite, chaque équipe : `✔` lit / `✘` + joueurs vivants, tes kills.
- Boss bar : phase (« Mort subite dans 2:00 »).
- Marchand : menu inventaire par catégories, prix colorés (vert si achetable), clic = achat, shift-clic = achat ×5 pour les blocs, achat impossible → son + tremblement du titre de l'item.
- Annonces : lit détruit (titre à l'équipe touchée + chat global), élimination d'équipe.

## 10. Visuels
`Shatter` du lit + `BlockCrack` pendant la casse ; hologrammes de générateurs (prochain spawn) ; effets de kill ; feux d'artifice couleur gagnante ; marqueur de lit allié (waypoint) si disponible.

## 11. Cas limites
- Lit cassé par explosion alors que `bedsBlastProof` = false → attribué au poseur de la TNT.
- Un joueur coincé dans un bloc à son respawn → téléportation sur le point le plus proche libre.
- Égalité stricte au timeout avec `MOST_ALIVE` → `DRAW`.
- Party de 4 en `2x4` → même équipe ; deux parties de 3 → équipes complétées par des solos.

## 12. Recette
- [ ] Achat de chaque catégorie, monnaie insuffisante, outils à paliers conservés à la mort.
- [ ] Casse de lit → plus de respawn → élimination → victoire.
- [ ] TNT : ne casse que les blocs posés.
- [ ] Mort subite à l'heure dite.
- [ ] Combat 1.8 conforme (`10-pvp-legacy.md` §7).
