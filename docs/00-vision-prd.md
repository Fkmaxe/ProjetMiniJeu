# 00 — Vision & exigences (PRD)

## 1. Vision

Un réseau mini-jeux Minecraft **Java 26.1** capable de tourner **sans intervention humaine** (les parties se
créent, se remplissent, se lancent et se recyclent seules) tout en laissant les admins **lancer, configurer et
superviser** des parties à la demande. La plateforme doit rendre l'ajout d'un mini-jeu **trivial** pour un
développeur et la configuration d'un jeu **évidente** pour un admin non-développeur.

## 2. Personas

| Persona | Besoin principal |
|---|---|
| **Joueur** | Rejoindre une partie en 1 clic, jouer avec ses amis, voir sa progression et les classements. |
| **Chef de party** | Emmener son groupe dans la même partie / la même équipe ; lancer une partie privée. |
| **Modérateur / Helper** | Observer une partie, téléporter, gérer les joueurs (hors scope sanctions v1). |
| **Admin de jeu** | Régler les paramètres d'un mode (temps, prix, générateurs…) sans toucher un fichier. |
| **Builder** | Créer/éditer une map et y placer les points (spawns, lits, générateurs) avec des outils guidés. |
| **Admin système** | Ajouter des serveurs, voir la charge, déployer une nouvelle version. |
| **Développeur de jeu** | Créer un nouveau mode en ne connaissant que `nexus-api`. |

## 3. Exigences fonctionnelles

### Plateforme & parties
- **FR-01** Lancement **autonome** : pour chaque (mode, variante) actif, l'orchestrateur maintient au moins `minWaitingInstances` instances joignables ; quand le seuil de joueurs est atteint, un compte à rebours démarre (accéléré quand l'instance se remplit), puis la partie démarre.
- **FR-02** Lancement **à la demande** : un admin (ou un joueur autorisé) crée une instance via commande ou menu en choisissant mode, variante, map, preset de config, privée/publique ; il peut forcer le démarrage, l'arrêt, ajouter/retirer des joueurs.
- **FR-03** Parties **privées** avec code d'invitation, et réglages surchargeables par l'hôte (sous-ensemble autorisé par l'admin).
- **FR-04** Répartition sur **plusieurs serveurs** : N lobbys, N serveurs de jeu, chaque serveur de jeu héberge plusieurs instances simultanées de un ou plusieurs modes.
- **FR-05** **Reconnexion** : un joueur déconnecté pendant une partie en cours peut la rejoindre pendant `rejoinWindowSeconds` (si le mode l'autorise).
- **FR-06** **Spectateur** : les joueurs éliminés et le staff peuvent observer ; `/spectate <joueur>` pour le staff.
- **FR-07** Retour automatique au lobby en fin de partie, avec proposition « Rejouer » (re-file immédiate, party incluse).

### Nouveaux jeux
- **FR-10** Un nouveau mini-jeu s'ajoute comme **module indépendant** ne dépendant que de `nexus-api`, sans modifier le core.
- **FR-11** L'API fournit : phases, équipes, kits, modules réutilisables (mort dans le vide, respawn, générateurs, boutique, lit, PvP legacy, spectateur…), scoreboard, stats, schéma de map, schéma de config.
- **FR-12** Le mode apparaît **automatiquement** dans les sélecteurs du lobby, l'admin UI et les leaderboards une fois chargé et activé.

### Configuration admin
- **FR-20** Chaque mode expose une **config typée**, rendue automatiquement en interface in-game (menus + Dialogs) : catégories, libellés FR, descriptions, bornes, valeurs par défaut, aperçu des modifications.
- **FR-21** Configs **versionnées** en BDD, avec **presets** (ex. « Classique », « Rapide », « Événement »), historique, retour arrière, et application « aux prochaines parties ».
- **FR-22** **Éditeur de map** guidé : le schéma de map du mode liste les marqueurs requis ; le builder les place avec des outils ; la map est validée puis publiée.
- **FR-23** **Hub admin** : serveurs (charge, TPS/MSPT), instances en cours (forcer démarrage/arrêt, observer), modes (activer/désactiver), configs, maps, rôles.

### Stats & classements
- **FR-30** Pour chaque mode et variante : victoires, défaites, parties jouées, + stats spécifiques déclarées par le jeu (kills, lits détruits, meilleur temps…).
- **FR-31** Leaderboards **global, mensuel, hebdomadaire**, consultables en jeu (menu, hologrammes du lobby) ; records (« meilleur temps » Dropper).
- **FR-32** Historique des parties (match + participants + résultat) consultable par le joueur (`/stats`, `/historique`).

### Rôles & permissions
- **FR-40** Rôles avec héritage, poids, préfixe/couleur, permissions (jokers et négations), attribution temporaire.
- **FR-41** Permissions appliquées sur **tous** les serveurs (Paper et Velocity) et propagées en temps réel.
- **FR-42** Gestion par commandes et par interface.

### Social
- **FR-50** Amis : demandes, acceptation, suppression, liste avec statut en ligne / serveur / jeu en cours, messages privés inter-serveurs.
- **FR-51** Party : création, invitations (expiration), expulsion, transfert de chef, chat de party, la party rejoint les files **ensemble** et est placée dans la **même équipe** si la taille le permet.
- **FR-52** Préférences : accepter/refuser demandes d'amis et invitations de party.

### Jeux de départ
- **FR-60** **Dropper** (cf. `games/dropper.md`).
- **FR-61** **Rush** façon Funcraft (cf. `games/rush.md`).
- **FR-62** **Bedwars** classique (cf. `games/bedwars.md`).
- **FR-63** PvP **1.8** activable par mode (cf. `10-pvp-legacy.md`).
- **FR-64** Utilisation des capacités visuelles modernes (cf. `11-visuals-animations.md`).

## 4. Exigences non fonctionnelles

| ID | Exigence | Cible |
|---|---|---|
| **NFR-01** | MSPT d'un serveur de jeu à pleine charge nominale | p95 < 30 ms, p99 < 45 ms |
| **NFR-02** | Aucun I/O sur main thread | 0 occurrence (revue + profilage spark) |
| **NFR-03** | Temps entre « file atteinte » et téléportation dans l'instance | < 2 s (monde pré-chargé) |
| **NFR-04** | Création d'une instance (monde depuis template en cache) | < 500 ms sur le main thread, cumulé |
| **NFR-05** | Propagation d'un changement de permission / config | < 2 s sur tout le réseau |
| **NFR-06** | Perte d'un serveur de jeu | les joueurs reviennent au lobby, l'orchestrateur réalloue, aucune stat corrompue |
| **NFR-07** | Perte de Redis | les parties en cours se terminent ; nouvelles files suspendues ; reprise automatique |
| **NFR-08** | Perte de Postgres | les parties continuent ; écritures de stats mises en file locale (disque) et rejouées |
| **NFR-09** | Ajout d'un jeu simple (type Dropper) | < 500 lignes de code de jeu |
| **NFR-10** | Couverture de tests de la logique pure (calculs, économie, matchmaking) | ≥ 80 % |
| **NFR-11** | i18n | FR (défaut) + EN |
| **NFR-12** | Mise à l'échelle | ajouter un serveur de jeu = créer un serveur Pterodactyl depuis l'egg (ou l'ajouter au pool), zéro config manuelle côté proxy |

## 5. Hors scope v1

Panneau web d'administration (l'architecture le permet : configs et schémas sont en JSON), boutique en ligne,
sanctions (ban/mute) complètes, anti-cheat, Bedrock/Geyser, cosmétiques payants, Folia.

## 6. Hypothèses (à confirmer — voir §7)

- **H1** Serveur **premium** (`online-mode` géré par Velocity). Pas de support crack.
- **H2** Clients en **26.1.x** uniquement (pas de ViaVersion dans la v1) — l'aspect « 1.8 » est **côté gameplay**.
- **H3** Déploiement sur **Pterodactyl** (Panel + Wings) : chaque serveur Nexus est un serveur Pterodactyl créé depuis les eggs du dépôt ; Postgres/Valkey hors panel ; l'orchestrateur démarre/arrête un pool de serveurs de jeu via l'API du panel (cf. `17-deploiement-pterodactyl.md`, ADR-010).
- **H4** **Rush** = variante Funcraft : 2 équipes (configurable jusqu'à 4), lits, ressources bronze/fer/or générées en base, boutique PNJ, map où les bases sont reliées.
- **H5** **Bedwars** « à l'ancienne » = format classique 2016-2018 : îles dans le vide, 8×1 / 8×2 / 4×3 / 4×4, générateurs fer/or en base, diamant/émeraude au centre, boutique d'objets + améliorations d'équipe, lit détruit = plus de respawn.
- **H6** Volume cible initial : ~200 joueurs simultanés, extensible à 1 000+ en ajoutant des serveurs.

## 7. Questions ouvertes

| # | Question | Impact | Défaut retenu en attendant |
|---|---|---|---|
| Q1 | Rush : correspond-il bien à H4 (monnaies, taille d'équipes, TNT, map) ? | Spec Rush | H4, tout configurable |
| Q2 | Bedwars : version Hypixel classique (H5) ou l'ancien Bedwars bronze/fer/or ? | Spec Bedwars | H5 |
| Q3 | Faut-il ViaVersion pour accepter des clients plus anciens ? | Proxy, tests | Non |
| Q4 | Pack de ressources serveur (modèles d'items custom, sons) ? | Visuels | Optionnel, rien d'obligatoire |
| Q5 | Panneau web v2 ? | Garder les schémas JSON propres | Oui, prévu |
| Q6 | Nombre de serveurs physiques / RAM disponible ? | Dimensionnement | 1 machine 8 vCPU / 16 Go en dev |
