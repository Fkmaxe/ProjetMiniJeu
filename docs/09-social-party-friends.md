# 09 — Social : amis, party, messages

Tout le social vit dans **`nexus-proxy`** (le joueur garde sa party et ses amis en changeant de serveur).
Les serveurs Paper ne font qu'**afficher** (menus du lobby, couleur des membres de party) via les messages `party.updated`.
Multi-proxy : l'état est dans Redis/Postgres, chaque proxy ne livre qu'aux joueurs qu'il détient.

## 1. Amis

| Action | Commande | Règles |
|---|---|---|
| Demander | `/ami ajouter <pseudo>` (`/friend add`) | pseudo résolu en BDD (joueur déjà venu) ; refus si préférence « demandes désactivées » ; cooldown 10 s ; limite 100 amis (200 VIP) ; demande croisée existante → acceptation automatique |
| Accepter / refuser | `/ami accepter|refuser <pseudo>` + boutons cliquables dans le message | demande supprimée |
| Supprimer | `/ami retirer <pseudo>` | confirmation cliquable |
| Lister | `/ami liste [page]` ou menu lobby | en ligne d'abord : serveur/jeu en cours (`presence:*`), puis hors-ligne avec « vu il y a 3 j » |
| Rejoindre | `/ami rejoindre <pseudo>` | si l'ami est au lobby → même lobby ; s'il est dans une instance joignable → file vers cette instance ; sinon refus |

- Notifications de connexion/déconnexion des amis (préférence activable) via `social.friend-status`.
- Requête « mes amis » : 1 requête SQL (union `player_a`/`player_b`) au login, mise en cache sur le proxy.

## 2. Messages privés

`/msg <pseudo> <texte>`, `/r <texte>` (dernier correspondant). Livraison via `social.message` ; le texte est **échappé** (pas de MiniMessage du joueur). Préférences : MP de tous / amis seulement / personne. Le staff voit un `/socialspy` optionnel (perm `nexus.staff.socialspy`, v1.1).

## 3. Party

### Modèle (Redis)

```
party:<partyId>         HASH { leader, members: [uuid...], createdAt, open: false, chatMode }
party:member:<uuid>     partyId
party:invite:<partyId>:<uuid>  inviterId   (TTL 60 s)
```
TTL glissant de 6 h rafraîchi à chaque activité ; une party dont tous les membres sont hors ligne depuis 5 min est dissoute.

### Commandes

| Commande | Effet |
|---|---|
| `/party inviter <pseudo>` (`/p invite`) | crée la party si besoin (le créateur est chef) ; invitation cliquable 60 s ; taille max via `nexus.party.size.<n>` du **chef** (défaut 4) |
| `/party accepter <pseudo>` | rejoint ; un joueur ne peut être que dans une party |
| `/party quitter` | si chef : transfert au membre le plus ancien |
| `/party expulser <pseudo>` | chef seulement |
| `/party chef <pseudo>` | transfert |
| `/party dissoudre` | chef |
| `/party liste` | membres, statut, serveur |
| `/party warp` | téléporte les membres au serveur/lobby du chef (pas dans une instance en cours) |
| `/party chat` / `@texte` | bascule ou message ponctuel au chat de party |
| `/party ouvrir` | party publique (`/party rejoindre <chef>` sans invitation) — v1.1 |

### Party et matchmaking

- Seul le **chef** peut mettre en file ; tous les membres **en ligne et non en partie** sont emmenés. Un membre en partie bloque la file (« X est en partie »).
- Taille ≤ `teamSize` → même équipe garantie. Taille > `teamSize` → refus sauf `allowSplitParties` (ex. Dropper solo : toute la party dans la même instance, chacun pour soi).
- Si un membre quitte la file (déconnexion), toute l'entrée sort de la file et le chef est prévenu.
- Fin de partie : « Rejouer » du chef remet toute la party en file.
- Partie privée : les membres de la party sont invités automatiquement.

## 4. Chats

| Canal | Portée | Accès |
|---|---|---|
| Global | serveur courant (lobby) / instance (en jeu) | tous |
| Équipe | équipe de l'instance (module `TeamChatModule`) | membres |
| Party | membres de la party, cross-serveur (`party.chat`) | membres |
| Staff | tous les serveurs (`staff.chat`) | `nexus.staff.chat` |

Format du chat : préfixe du rôle d'affichage + pseudo coloré + message (Adventure `ChatRenderer`).

## 5. Préférences joueur (`players.settings`)

```json
{ "friendRequests": "ALL|NONE", "partyInvites": "ALL|FRIENDS|NONE", "privateMessages": "ALL|FRIENDS|NONE",
  "friendNotifications": true, "locale": "fr_FR", "showPlayersInLobby": true }
```
Éditables via le menu **Paramètres** du lobby (Dialog avec interrupteurs/choix).
