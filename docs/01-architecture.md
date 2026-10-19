# 01 — Architecture

## 1. Vue d'ensemble

```mermaid
flowchart LR
    subgraph Clients
        P1[Joueurs 26.1.x]
    end

    subgraph Edge
        V1[Velocity #1<br/>nexus-proxy]
        V2[Velocity #2<br/>nexus-proxy]
    end

    subgraph Lobbys
        L1[Paper lobby-1<br/>nexus-core + nexus-lobby]
        L2[Paper lobby-2]
    end

    subgraph Jeux["Serveurs de jeu (multi-instances)"]
        G1[Paper game-1<br/>nexus-core + dropper + rush]
        G2[Paper game-2<br/>nexus-core + bedwars]
        G3[Paper game-n ...]
    end

    ED[Paper editor<br/>nexus-core + tous les jeux<br/>mode édition de maps]

    O[nexus-orchestrator<br/>registre · files · allocation · scaling]
    R[(Valkey / Redis<br/>pub/sub · présence · files · ZSET)]
    DB[(PostgreSQL<br/>joueurs · rôles · stats · configs · maps)]
    S[(Stockage templates de maps<br/>volume partagé / S3-compatible)]

    P1 --> V1 & V2
    V1 & V2 --> L1 & L2 & G1 & G2 & G3 & ED
    V1 & V2 <--> R
    L1 & L2 & G1 & G2 & G3 & ED <--> R
    O <--> R
    O --> DB
    L1 & G1 & G2 & ED --> DB
    V1 & V2 --> DB
    G1 & G2 & G3 --> S
    ED --> S
    O -. "API Pterodactyl (start/stop du pool)" .-> G3
```

### Rôle de chaque composant

| Composant | Type | Responsabilités | Ne fait PAS |
|---|---|---|---|
| **nexus-proxy** | Plugin Velocity | Authentification, routage initial vers un lobby, exécution des transferts demandés par l'orchestrateur, party, amis, `/msg`, permissions côté proxy, présence (joueur → proxy/serveur). | Choisir une instance de jeu. |
| **nexus-orchestrator** | Service Java 25 autonome (serveur Pterodactyl, egg dédié) | Registre des serveurs (heartbeats), registre des instances, **files d'attente & matchmaking**, politique « toujours N instances joignables », création à la demande, **scaling** (provider pterodactyl en prod, static en dev), migrations Flyway, recalcul des leaderboards. | Parler aux clients Minecraft. |
| **nexus-core** | Plugin Paper (tous les serveurs Paper) | Profils joueurs, permissions, i18n, framework GUI/Dialog, **runtime des instances** (chargement monde, phases, équipes, modules), écriture des stats, admin UI, éditeur de maps (serveur `editor`). | Logique propre à un jeu. |
| **nexus-lobby** | Plugin Paper (lobbys) | Spawn, hotbar (sélecteur de jeux, profil, amis, party), PNJ/portails de jeux, hologrammes de leaderboards, parcours/attente. | — |
| **game-*** | Plugins Paper | Règles d'un mini-jeu via `nexus-api`. | I/O, files, monde, BDD. |
| **Valkey** | Infra | Bus temps réel, état éphémère (présence, parties, files, réservations), caches de leaderboards. | Stockage durable. |
| **PostgreSQL** | Infra | Données durables. | Bus de messages. |
| **Stockage templates** | Infra | Archives de mondes publiées (`.tar.zst`) + checksum. | — |

### Profils de serveurs

Chaque serveur Paper démarre avec `NEXUS_SERVER_TYPE` = `lobby` | `game` | `editor` et, pour `game`,
la liste des modes qu'il héberge est **déduite des plugins de jeu chargés**. Il s'annonce à l'orchestrateur
avec ses capacités (cf. `04-messaging-protocol.md` → `server.heartbeat`).

## 2. Modèle d'exécution d'un serveur de jeu

```mermaid
flowchart TB
    subgraph PaperGame["JVM Paper game-1"]
        Core[nexus-core<br/>InstanceManager]
        subgraph I1["Instance bw-7f3a (monde bw-7f3a)"]
            Ph1[Phase: InGame]
            M1[Modules: Generators, Shop, BedDefense, LegacyCombat...]
        end
        subgraph I2["Instance dr-91c2 (monde dr-91c2)"]
            Ph2[Phase: Countdown]
        end
        subgraph I3["Instance dr-a004 (pré-chargée, vide)"]
            Ph3[Phase: Waiting]
        end
        Core --> I1 & I2 & I3
        WP[WorldProvider<br/>cache local des templates]
        Core --> WP
    end
```

- **Une instance = un monde** (créé depuis un template, jamais sauvegardé, détruit à la fin).
- Le main thread Paper tique toutes les instances ; le **budget MSPT** est partagé → l'orchestrateur ne place une nouvelle instance que si le serveur a de la marge (score de capacité, cf. `05`).
- Les événements Bukkit sont routés vers l'instance concernée par un **dispatcher** (index `World → Instance`, `UUID joueur → Instance`) : un jeu ne voit **que** les événements de son instance.

## 3. Flux principaux

### 3.1 Connexion d'un joueur

```mermaid
sequenceDiagram
    participant C as Client
    participant V as nexus-proxy
    participant R as Redis
    participant DB as Postgres
    participant L as lobby (nexus-core)

    C->>V: Login (Mojang auth)
    V->>DB: upsert player (uuid, name, last_seen) [async]
    V->>DB: charger rôles/permissions [async, cache Caffeine]
    V->>R: SET presence:<uuid> {proxy, server:null} EX 60
    V->>R: GET party:member:<uuid> (reprise de party)
    V->>V: choisir lobby (moins chargé, via registre Redis)
    V->>L: connect
    L->>DB: AsyncPlayerPreLogin → charger profil + stats résumées
    L->>R: SET presence:<uuid>.server = lobby-1
    L-->>C: Spawn lobby, hotbar, scoreboard
    V->>R: PUBLISH social.friend-status (online)
```

### 3.2 File d'attente → partie (mode autonome)

```mermaid
sequenceDiagram
    participant C as Joueur (lobby)
    participant V as nexus-proxy
    participant O as Orchestrateur
    participant R as Redis
    participant G as game-2 (nexus-core)

    C->>V: clic « Bedwars 4x4 » (message plugin lobby→proxy ou commande /play)
    V->>R: PUBLISH queue.join {players:[party], mode, variant}
    R->>O: queue.join
    O->>O: matchmaking: instance WAITING la plus remplie<br/>où la party tient dans une équipe
    O->>R: SET reservation:<instanceId>:<uuid> EX 15
    O->>R: PUBLISH proxy.<proxyId>.transfer {uuid, server:game-2, instanceId}
    R->>V: transfer
    V->>G: connect joueur
    G->>R: GET reservation → OK, supprime
    G->>G: InstanceManager.join(instance, player) → WaitingPhase
    G->>R: PUBLISH instance.update {players:+1}
    Note over G: seuil min atteint → CountdownPhase<br/>(accélérée si pleine)
    G->>R: PUBLISH instance.update {state:RUNNING}
    O->>O: plus d'instance WAITING pour bedwars 4x4<br/>→ demande create à un serveur avec capacité
```

### 3.3 Fin de partie

1. Le jeu appelle `ctx.end(GameResult)` (gagnants, classement, stats).
2. `EndPhase` : animation de victoire (5–10 s configurables), messages récap.
3. `nexus-core` construit un `MatchReport` et l'écrit **en une transaction** (`matches`, `match_participants`, upsert `player_stats` × périodes, `player_records`). En cas d'échec BDD → **journal local** (`plugins/NexusCore/pending-reports/*.json`) rejoué au redémarrage / toutes les 30 s.
4. Publie `match.finished` → l'orchestrateur invalide les caches de leaderboards concernés, le proxy propose « Rejouer ».
5. Les joueurs sont renvoyés au lobby (ou re-filés directement s'ils ont cliqué « Rejouer »).
6. Instance → `CLEANUP` : désenregistrement listeners/tâches, suppression entités, `unloadWorld(false)`, suppression du dossier. → `DESTROYED`, `instance.removed` publié.

### 3.4 Création à la demande

`/game create bedwars 4x4 map=Lighthouse preset=Rapide private` (ou menu admin) →
le core envoie `instance.create-request` à l'orchestrateur (avec `requestedBy`), qui choisit le serveur, crée
l'instance (`flags: PRIVATE, MANUAL_START`), renvoie un **code d'invitation** et téléporte le demandeur.
`/game start` force le démarrage même sous le minimum ; `/game stop` termine sans enregistrer de stats
(`GameResult.aborted()`).

## 4. Pannes et dégradations

| Panne | Détection | Comportement |
|---|---|---|
| Serveur de jeu crashé | heartbeat absent > 10 s | Orchestrateur marque `DEAD`, supprime ses instances, renvoie les joueurs (le proxy reçoit `KickedFromServerEvent` → lobby). Matchs en cours : pas de stats (aucun rapport émis). |
| Orchestrateur indisponible | heartbeat absent | Files suspendues (message « Matchmaking en maintenance »), parties en cours continuent. Les créations à la demande échouent proprement. Un 2e orchestrateur en **standby** prend la main via verrou Redis (`orchestrator:leader`, `SET NX PX`). |
| Redis indisponible | erreurs Lettuce | Parties en cours continuent ; joueurs restent où ils sont ; reconnexion automatique, ré-annonce complète des états au retour (`server.resync`). |
| Postgres indisponible | erreurs Hikari | Connexions de nouveaux joueurs : profil par défaut en lecture seule + message ; rapports de match en journal local. |
| Proxy crashé | — | Les joueurs de ce proxy sont déconnectés ; `presence:*` expire (TTL 60 s, rafraîchi toutes les 20 s). |

## 5. Sécurité

- Velocity **modern forwarding** avec secret ; les serveurs Paper n'écoutent que sur le réseau interne.
- Redis/Postgres jamais exposés ; Redis avec ACL et mot de passe ; un utilisateur Postgres applicatif sans droits DDL (sauf l'orchestrateur pour Flyway).
- Toute commande admin vérifie la permission **côté serveur qui exécute** (jamais de confiance dans un message Redis venant d'un client).
- Les messages Redis portent `origin` (id serveur) ; les commandes sensibles (`instance.force-stop`, `perm.*`) ne sont acceptées que si l'origine est un serveur enregistré.

## 6. Évolutions prévues

- Panneau web (lecture des mêmes schémas JSON de config, API REST exposée par l'orchestrateur).
- Provider Kubernetes pour le scaling (Pterodactyl est le provider de production, cf. `17-deploiement-pterodactyl.md`).
- Saisons de classement (`seasons` déjà prévu dans le schéma).
