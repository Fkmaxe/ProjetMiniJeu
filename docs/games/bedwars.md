# Bedwars (`bedwars`) — classique

> Statut : **à valider** (cf. Q2 dans `00-vision-prd.md`) · Version de spec : 1 · PvP : legacy 1.8

## 1. Pitch
Le Bedwars « à l'ancienne » : îles flottantes dans le vide, une par équipe, avec un lit et un générateur fer/or ;
des îles de diamant et un centre d'émeraudes ; une boutique d'objets et une boutique d'améliorations d'équipe.
Protéger son lit, ponter vers les autres, casser leurs lits, être la dernière équipe debout.

## 2. Variantes
| Variante | Équipes × taille | Min joueurs | File auto |
|---|---|---|---|
| `8x1` Solo | 8 × 1 | 4 | oui |
| `8x2` Doubles | 8 × 2 | 6 | oui |
| `4x3` | 4 × 3 | 6 | non |
| `4x4` | 4 × 4 | 8 | oui |

Couleurs : rouge, bleu, vert, jaune, cyan, blanc, rose, gris (ordre fixe).

## 3. Déroulé
1. **Waiting** : plateforme d'attente au-dessus de la map (supprimée virtuellement au départ : elle est hors de la zone de jeu).
2. **Countdown** : standard.
3. **InGame** avec **événements chronométrés** (configurables) :

| Temps (défaut) | Événement |
|---|---|
| 0:00 | Diamant I, Émeraude I |
| 6:00 | Diamant II |
| 12:00 | Émeraude II |
| 18:00 | Diamant III |
| 24:00 | Émeraude III |
| 30:00 | **Destruction de tous les lits** |
| 40:00 | **Mort subite** : bordure qui rétrécit (option) ou dragons (v2) |
| 50:00 | Fin : égalité (ou équipe avec le plus de joueurs vivants, option) |

4. **End** : victoire de la dernière équipe en vie.

## 4. Règles
- Lit debout → respawn après `respawnSeconds` (5) en spectateur au-dessus de l'île, avec compte à rebours. Lit détruit → élimination à la mort suivante (**final kill**).
- Mort : l'inventaire de **ressources** (fer, or, diamant, émeraude) va au tueur ; le reste est perdu sauf **armure achetée** (permanente, réappliquée au respawn) et **outils** (rétrogradés d'un palier, jamais sous le palier 1) ; l'épée revient en bois (+ amélioration Tranchant d'équipe).
- Boutique d'objets (PNJ « Marchand ») et boutique d'équipe (PNJ « Améliorations ») sur chaque île ; menus inventaire.
- Construction : seulement les blocs posés sont cassables (+ lits ennemis), hauteur max (méta), zones protégées (spawns, générateurs, PNJ, rayon autour des générateurs centraux).
- **Laine** couleur d'équipe ; **verre anti-explosion** résiste à la TNT ; **pierre de l'End**, **obsidienne** plus résistantes (vanilla).
- Pas de craft, pas de drop d'items non-ressources (option), coffre d'équipe et coffre ender (option).
- Vide : mort sous `void_y`, attribuée au dernier attaquant.
- Équipe éliminée → son île reste, ses générateurs s'arrêtent (option `stopEliminatedGenerators`).

### Boutique d'objets (défaut, éditable)
| Catégorie | Offre | Prix |
|---|---|---|
| Blocs | 16 laine | 4 fer |
| Blocs | 16 argile (terracotta) | 12 fer |
| Blocs | 4 verre anti-explosion | 12 fer |
| Blocs | 12 pierre de l'End | 24 fer |
| Blocs | 16 planches | 4 or |
| Blocs | 4 obsidienne | 4 émeraudes |
| Armes | Épée en pierre / fer / diamant | 10 fer / 7 or / 4 émeraudes |
| Armes | Bâton de recul (Recul I) | 5 or |
| Armure | Mailles / Fer / Diamant (permanente, jambières+bottes) | 40 fer / 12 or / 6 émeraudes |
| Outils | Pioche (4 paliers, Efficacité), Hache (4 paliers), Cisailles | fer/or croissants |
| Arcs | Arc / Arc Puissance I / Arc Puissance I Frappe I / Flèches ×8 | 12 or / 24 or / 6 émeraudes / 2 or |
| Potions | Vitesse II 45 s, Saut V 45 s, Invisibilité 30 s | 1 / 1 / 2 émeraudes |
| Utilitaires | Pomme d'or, Boule de feu, TNT, Perle de l'Ender, Seau d'eau, Œuf-pont, Éponge | 3 or, 40 fer, 4 or, 4 émeraudes, 1 or, 1 émeraude, 3 or |

### Améliorations d'équipe (défaut)
| Amélioration | Effet | Prix (diamants) |
|---|---|---|
| Épées aiguisées | Tranchant I pour l'équipe | 4 (solo/doubles : 2) |
| Armure renforcée I–IV | Protection I–IV | 2 / 4 / 8 / 16 |
| Mineur fou I–II | Célérité I–II | 2 / 4 |
| Forge I–IV | Générateur d'île +50 % / +100 % / émeraudes / +200 % | 2 / 4 / 6 / 8 |
| Guérison de base | Régénération I dans l'île | 1 |
| Piège (file de 3) | « C'est un piège » (Aveuglement+Lenteur), Contre-offensive, Alarme, Fatigue | 1, puis 2, puis 4 |

> Les **effets** (Tranchant, Protection, Célérité, Régénération) sont appliqués aux items/joueurs de l'équipe par le jeu ; leurs valeurs sont celles du **combat 1.8** (`10-pvp-legacy.md`).

## 5. Config (`BedwarsConfig`)
| Catégorie | Option | Type | Bornes | Défaut | Hôte ? |
|---|---|---|---|---|---|
| Général | `events` | liste de `TimedEvent(atSeconds, type)` (DIAMOND_TIER, EMERALD_TIER, BED_DESTRUCTION, SUDDEN_DEATH, GAME_END) | 1–20 | tableau §3 | non |
| Général | `timeoutRule` | enum | — | DRAW | oui |
| Respawn | `respawnSeconds` | int | 0–15 | 5 | oui |
| Respawn | `toolDowngradeOnDeath` | bool | — | true | oui |
| Générateurs d'île | `islandIron` / `islandGold` | `GeneratorDef` | — | fer 1 s ×1 (max 48), or 6 s ×1 (max 12) | non |
| Générateurs centraux | `diamondTiers` / `emeraldTiers` | liste d'intervalles (s) | 1–5 | 30/23/12 ; 65/50/35 | non |
| Générateurs | `shareIslandResources` | bool | — | true | oui |
| Boutique | `itemShop`, `teamUpgrades`, `traps` | catalogues | — | §4 | non |
| Construction | `maxBuildHeightOffset` | int | 0–60 | 20 (au-dessus du point le plus haut des îles) | oui |
| Construction | `blastProofGlass`, `fireballs`, `bridgeEggs` | bool | — | true | oui |
| Fin | `suddenDeathBorder` | bool | — | true | oui |
| Combat | `combatProfile` / `knockbackProfile` / `fireballKnockback` | choix / choix / double 0–3 | — | legacy-1.8 / 1.8-vanilla / 1.0 | oui |
| Visuels | `bedShatter`, `generatorHolograms`, `killEffects`, `finalKillLightning` | bool | — | true | non |

Presets : `classic` (défauts), `rush` (générateurs ×2, événements ÷2), `lucky` (v2).

## 6. Schéma de map
| Élément | Type | Cardinalité | Rôle |
|---|---|---|---|
| `waiting_spawn` | point | 1 | plateforme d'attente |
| `team_spawn` | point + rotation, `perTeam` | = nb équipes | spawn |
| `team_bed` | bloc, `perTeam` | = nb équipes | lit |
| `island_generator` | point, `perTeam` | 1 par équipe | fer+or |
| `item_shop` / `upgrade_shop` | point + rotation, `perTeam` | 1 par équipe chacun | PNJ |
| `team_chest` | bloc, `perTeam` | 0–1 par équipe | coffre d'équipe |
| `base_region` | région, `perTeam` | 1 par équipe | rayon de guérison / pièges / protection |
| `diamond_generator` | point | ≥ 2 | îles diamant |
| `emerald_generator` | point | ≥ 1 | centre |
| `protected` | région | ≥ 0 | non constructible (autour des générateurs centraux, etc.) |
| `void_y`, `build_max_y`, `border_center`, `border_radius` | méta | — | limites |

Validation : cardinalités par variante (une map 8 équipes peut servir à `8x1` et `8x2` ; une map 4 équipes à `4x3`/`4x4`).

## 7. Modules
`LegacyCombatModule`, `BedModule`, `GeneratorModule` (îles + centraux à paliers + hologrammes), `ShopModule` (objets + améliorations), `TeamUpgradesModule` (spécifique Bedwars → dans le jeu, candidat à l'API si un 2e jeu en a besoin), `TrapModule` (idem), `RespawnModule`, `VoidDeathModule`, `BuildRulesModule`, `KitModule`, `TeamChatModule`, `SpectatorModule`, `WorldBorderModule`, `TimeLimitModule`.

## 8. Stats
| Clé | Agrégation | Leaderboard |
|---|---|---|
| `kills`, `final_kills`, `beds_broken` | SUM | oui |
| `deaths`, `final_deaths`, `beds_lost` | SUM | non |
| `resources_iron`, `resources_gold`, `resources_diamond`, `resources_emerald` | SUM | non |
| (auto) `wins`, `losses`, `games_played`, `win_streak`, `best_win_streak` | | oui (wins, best_win_streak) |

## 9. UI
- Sidebar : prochain événement + timer, liste des équipes (`✔` lit / `nombre` joueurs restants / `✘` éliminée, « TOI » sur la sienne), kills / final kills / lits.
- Tab list colorée par équipe, tri par équipe.
- Boutique : 1er écran « Achats rapides » (9 favoris personnalisables plus tard, défaut fixe) + catégories ; prix en couleur de la ressource.
- Améliorations : niveaux actuels, prochain prix, file des pièges.

## 10. Visuels
Générateurs centraux : `ItemDisplay` tournant (diamant/émeraude) + `TextDisplay` « Diamant II — 12 s » ; `Shatter` du lit + `BlockCrack` progressif ; éclair visuel sur final kill (option, `strikeLightningEffect`) ; feux d'artifice ; annonce des paliers (titre + son) ; pièges : titre d'alerte + son à l'équipe.

## 11. Cas limites
- Lit détruit pendant qu'un joueur attend son respawn → le respawn en cours est **honoré** (il revient), sa prochaine mort est finale.
- Équipe vide dès le départ (solo 8×1 à 6 joueurs) → l'île et son lit sont retirés (lit supprimé, générateur arrêté).
- Boule de feu : knockback configurable, dégâts au lanceur réduits, ne casse que les blocs posés.
- Œuf-pont : trajectoire posant de la laine derrière l'œuf (tâche par projectile, 1 tick, max 40 blocs).
- Piège déclenché par un joueur invisible → le déclenche quand même (et le révèle).

## 12. Recette
- [ ] 8×1 à 4 joueurs : îles vides retirées.
- [ ] Paliers diamant/émeraude aux bons moments, hologrammes corrects.
- [ ] Armure permanente + outils rétrogradés au respawn.
- [ ] Chaque amélioration et chaque piège.
- [ ] Destruction de tous les lits à 30:00, mort subite, fin à 50:00.
- [ ] Final kill, élimination d'équipe, victoire, stats écrites.
