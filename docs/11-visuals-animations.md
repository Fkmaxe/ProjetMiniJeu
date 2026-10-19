# 11 — Visuels & animations (capacités modernes 26.1)

Le gameplay de combat peut être « 1.8 », mais **tout le reste** profite des capacités récentes du client.
Règle : **aucun pack de ressources obligatoire** en v1 ; tout ce qui suit fonctionne en vanilla 26.1.
Un pack optionnel (modèles d'items, sons) pourra être proposé plus tard via `ResourcePackRequest` au niveau proxy.

## 1. Catalogue des briques (dans `nexus-api` → `fr.nexus.api.ui`, implémentées dans le core)

| Brique | Base technique | Usages |
|---|---|---|
| `Hologram` | `TextDisplay` (billboard, ombre, fond ARGB, alignement, opacité) + `ItemDisplay` optionnel | générateurs (« Diamant II — 12 s »), marqueurs d'éditeur, leaderboards du lobby, noms de PNJ |
| `Animated` | `Display#setInterpolationDuration` / `setInterpolationDelay` / `setTransformation`, `setTeleportDuration` | rotation des items de générateurs, apparition « pop » (scale 0 → 1), objets qui flottent, transitions fluides sans paquets à chaque tick |
| `Shatter` | 8–20 `BlockDisplay` avec transformations interpolées + gravité simulée sur 10–15 ticks | destruction de lit, explosion stylisée, victoire |
| `Npc` | entité **Mannequin** (apparence de joueur côté serveur) ou Villager sans IA (`setAI(false)`), `Interaction` entity pour la hitbox de clic | boutiques, sélecteurs du lobby |
| `ClickZone` | entité `Interaction` (largeur/hauteur libres) | boutons 3D du lobby, panneaux de leaderboards cliquables |
| `Sidebar` | scoreboard Adventure avec `NumberFormat.blank()` (plus de nombres rouges) et mise à jour différentielle | infos de partie |
| `TabList` | en-tête/pied Adventure, tri par rôle/équipe, couleurs d'équipe | — |
| `BossBar` | Adventure `BossBar` (progression, couleur, style) | compte à rebours, phase, mort subite, progression d'éditeur |
| `Title`/`ActionBar` | Adventure `Title.Times` | compte à rebours, éliminations, monnaie gagnée |
| `Dialog` | API Dialog de Paper | formulaires admin, paramètres joueur, confirmations, récap de fin de partie |
| `Waypoints` | barre de localisation (locator bar) / attribut de transmission des waypoints | voir ses coéquipiers ; lit allié comme repère (si l'API Paper l'expose — à vérifier en P3) |
| `Glow` | glowing par équipe (équipes scoreboard colorées) + `Player#sendPotionEffectChange`/visibilité par joueur | repérer les alliés, joueur traqueur |
| `Particles` | `Particle.DUST_COLOR_TRANSITION`, `TRAIL`, `FIREWORK`, `END_ROD`, builders Paper (`ParticleBuilder`) avec `receivers` limités à l'instance | effets de kill, chemins du Dropper, auras de générateurs |
| `Sounds` | Adventure `Sound` (catalogue centralisé `Sounds`) | feedback partout |
| `BlockCrack` | `Player#sendBlockDamage` | progression de casse du lit (visible par tous) |
| `Toast` | — (via avancement temporaire uniquement si simple ; sinon action bar) | records personnels |
| `ItemLook` | composants d'item : `item_name`, `lore`, `tooltip_display`, `enchantment_glint_override`, `item_model` (modèles vanilla existants), `custom_model_data` | items de boutique lisibles, outils d'éditeur |
| `Camera` | séquences de téléportations interpolées d'un `ItemDisplay` invisible + mode spectateur `setSpectatorTarget` | travelling d'intro de map (option), podium de victoire |

## 2. Effets par jeu (récap — détails dans `docs/games/*`)

| Moment | Dropper | Rush | Bedwars |
|---|---|---|---|
| Attente | aperçu de la map, panneau des records | choix d'équipe sur PNJ colorés | idem |
| Début | titre « Niveau 1 » + son | intro caméra (option) | idem |
| En jeu | traînée de particules derrière chaque joueur (couleur perso), anneau d'arrivée animé, chrono en action bar au ms | générateurs avec hologrammes, achat « pop » | générateurs diamant/émeraude : item tournant + compte à rebours, paliers annoncés |
| Événement fort | record battu : titre doré + feu d'artifice local | lit détruit : `Shatter` + éclair visuel sans dégâts + son du Wither | lit détruit : idem + boss bar « Lit détruit » pour l'équipe |
| Mort | — | effet de kill (particules) configurable | effet de kill, message coloré, final kill en gras |
| Fin | podium 1-2-3 (`Mannequin` + `TextDisplay`) | feux d'artifice couleur de l'équipe gagnante, récap Dialog | idem |

## 3. Règles de coût (performance)

1. **Display entities non persistantes** (`setPersistent(false)`), supprimées à la destruction de l'instance (suivies par `InstanceEntities`).
2. Privilégier l'**interpolation client** : une transformation + `interpolationDuration` remplace 20 téléportations. Une rotation continue = re-planifier une interpolation toutes les N ticks (ex. 1 tour en 40 ticks), pas une mise à jour par tick.
3. Hologrammes qui changent chaque seconde (timers) : mettre à jour le texte au plus **1×/s**, et seulement si le texte change.
4. Effets visibles **seulement par l'instance** (les mondes sont séparés, donc gratuit) ; effets par joueur via `setVisibleByDefault(false)` + `showEntity`.
5. Particules : `ParticleBuilder#receivers(rayon)` (32 blocs max), pas de boucle par tick sur tous les joueurs.
6. Budget par instance : ≤ 150 Display entities, ≤ 300 particules/s, ≤ 1 mise à jour de sidebar/s/joueur (le core diffère les lignes inchangées).
7. Toute animation est **désactivable** dans la config du jeu (`visuals.*`) — utile en cas de charge.

## 4. API (aperçu)

```java
Hologram h = ctx.ui().hologram(loc)
        .line(ctx.i18n().component("bedwars.gen.diamond", tier))
        .line(timer -> Component.text(timer.secondsLeft() + "s"))   // ligne dynamique rafraîchie 1×/s
        .item(ItemType.DIAMOND.createItemStack(), Spin.everyTicks(40))
        .spawn();

ctx.ui().shatter(bedBlocks, Shatter.defaults().color(team.color()).durationTicks(14));
ctx.ui().fireworks(winners, FireworkStyle.team(team.color()), 3);
```
