# 04 — Protocole de messages (Redis Pub/Sub)

Tous les messages inter-composants passent par Redis Pub/Sub, en JSON, avec une enveloppe commune.
Les records Java correspondants vivent dans `fr.nexus.common.protocol` ; **ce document et ce package doivent rester synchronisés**.

## 1. Enveloppe

```json
{
  "v": 1,
  "id": "0192f3c4-...",          // UUIDv7, pour déduplication et corrélation
  "type": "queue.join",
  "origin": "proxy-1",            // id du composant émetteur
  "ts": 1792412345678,            // epoch ms
  "replyTo": null,                // canal de réponse pour les requêtes
  "correlationId": null,          // id de la requête à laquelle on répond
  "payload": { ... }
}
```

```java
public record Envelope(int v, UUID id, String type, String origin, long ts,
                       @Nullable String replyTo, @Nullable UUID correlationId, JsonObject payload) {}
```

- `MessageBus#publish(channel, message)` — fire-and-forget.
- `MessageBus#request(channel, message, timeout)` → `CompletableFuture<Reply>` : met `replyTo = reply.<origin>` et attend la réponse avec le même `correlationId`. Timeout par défaut 3 s.
- Les handlers sont exécutés sur un **virtual thread** ; un handler qui touche l'API Bukkit doit repasser par `NexusScheduler.sync()`.
- Messages inconnus (`type` non mappé) : ignorés avec un log `debug` (compatibilité ascendante pendant les déploiements).
- `v` = version de l'enveloppe ; les payloads évoluent par **ajout de champs optionnels** uniquement.

## 2. Canaux

| Canal | Abonnés | Usage |
|---|---|---|
| `nexus:broadcast` | tous | annonces globales, `server.resync` |
| `nexus:orchestrator` | orchestrateur (leader uniquement traite) | requêtes vers l'orchestrateur |
| `nexus:server:<serverId>` | serveur ciblé | ordres pour un serveur Paper |
| `nexus:proxy` | tous les proxys | événements destinés au proxy qui détient le joueur (chaque proxy filtre sur ses joueurs) |
| `nexus:proxy:<proxyId>` | proxy ciblé | ordres pour un proxy |
| `nexus:servers` | tous les Paper + proxys | événements de registre (instances, configs, maps, perms) |
| `nexus:reply:<origin>` | émetteur d'une requête | réponses |

## 3. Catalogue des messages

Convention : `domaine.action`. **R** = requête (attend une réponse), **E** = événement.

### 3.1 Registre serveurs

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `server.heartbeat` | E | `nexus:orchestrator` (toutes les **2 s**) | `serverId, type (LOBBY/GAME/EDITOR), address, modes[] (modeId + variants[]), state (STARTING/READY/DRAINING/STOPPING), players, maxPlayers, instances, maxInstances, mspt (moy. 5 s), msptP95, freeMemoryMb, capacityScore (0-100)` |
| `server.shutdown` | E | `nexus:orchestrator` | `serverId, reason` |
| `server.drain` | E | `nexus:server:<id>` | `reason` — plus de nouvelles instances, s'arrête quand vide |
| `server.resync` | E | `nexus:broadcast` | — chacun ré-annonce son état complet (après reconnexion Redis ou failover orchestrateur) |

### 3.2 Instances

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `instance.create` | R | `nexus:server:<id>` (orchestrateur → serveur) | `instanceId, modeId, variantId, mapKey?, presetKey, flags[] (PRIVATE, MANUAL_START, UNRANKED), hostId?` → réponse `ok, error?, inviteCode?` |
| `instance.update` | E | `nexus:servers` + `nexus:orchestrator` | `instanceId, serverId, state, players, maxPlayers, joinable` (émis à chaque changement de state et au plus 1×/s pour les compteurs) |
| `instance.removed` | E | idem | `instanceId, serverId, reason` |
| `instance.create-request` | R | `nexus:orchestrator` (serveur/proxy → orchestrateur, à la demande) | `requestedBy, modeId, variantId, mapKey?, presetKey?, flags[], invitees[]` → `ok, instanceId, serverId, inviteCode` |
| `instance.force-start` | E | `nexus:server:<id>` | `instanceId, actor` |
| `instance.force-stop` | E | `nexus:server:<id>` | `instanceId, actor, reason` |
| `instance.join-code` | R | `nexus:orchestrator` | `playerId, code` → `ok, serverId, instanceId` |

### 3.3 Files & transferts

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `queue.join` | R | `nexus:orchestrator` | `players[] (uuid), partyId?, modeId, variantId, mapKey?` → `ok, position?, error? (ALREADY_QUEUED, MODE_DISABLED, PARTY_TOO_BIG, NOT_LEADER)` |
| `queue.leave` | E | `nexus:orchestrator` | `players[]` |
| `queue.status` | E | `nexus:proxy` | `players[], modeId, variantId, state (SEARCHING/FOUND), etaSeconds?` (affichage action bar) |
| `player.transfer` | E | `nexus:proxy` | `playerId, serverId, instanceId?, reservationToken?, reason` — le proxy qui détient le joueur exécute |
| `player.send-lobby` | E | `nexus:proxy` | `playerId, reason` |

### 3.4 Matchs & stats

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `match.started` | E | `nexus:orchestrator` | `matchId, instanceId, modeId, variantId, players[]` |
| `match.finished` | E | `nexus:servers` + `nexus:orchestrator` + `nexus:proxy` | `matchId, modeId, variantId, ranked, participants[] (uuid, outcome)` — émis **après** commit BDD |

### 3.5 Configs & maps

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `config.updated` | E | `nexus:servers` | `modeId, presetKey, configId, version` |
| `map.published` | E | `nexus:servers` | `modeId, mapKey, version` |
| `mode.toggled` | E | `nexus:servers` + `nexus:orchestrator` | `modeId, variantId?, enabled` |

### 3.6 Permissions & joueurs

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `perm.roles-changed` | E | `nexus:servers` + `nexus:proxy` | `roleIds[]` |
| `perm.player-changed` | E | idem | `playerId` |
| `player.updated` | E | idem | `playerId, fields[]` (locale, settings) |

### 3.7 Social

| Type | Sens | Canal | Payload |
|---|---|---|---|
| `social.message` | E | `nexus:proxy` | `from, to, text (brut, échappé côté réception)` |
| `social.friend-status` | E | `nexus:proxy` | `playerId, online, serverId?, modeId?` |
| `social.friend-request` | E | `nexus:proxy` | `from, to` |
| `party.updated` | E | `nexus:proxy` + `nexus:servers` | `partyId, leader, members[]` (les Paper l'utilisent pour l'affichage et le placement en équipe) |
| `party.disbanded` | E | idem | `partyId` |
| `party.chat` | E | `nexus:proxy` | `partyId, from, text` |
| `staff.chat` | E | `nexus:proxy` | `from, text` |

### 3.8 Messages plugin (lobby ↔ proxy)

Le lobby n'a pas besoin de connaître l'orchestrateur : les clics du sélecteur exécutent la commande proxy `/play <mode> <variant>`
via le **canal plugin** `nexus:main` (Plugin Messaging) ou, plus simplement, `player.performCommand` sur une commande enregistrée côté proxy.
Choix retenu : **commande proxy** (pas de message plugin à maintenir).

## 4. Fréquences & tailles

| Flux | Fréquence | Remarque |
|---|---|---|
| Heartbeats serveurs | 2 s × nb serveurs | ~300 octets |
| `instance.update` compteurs | ≤ 1/s par instance | agrégé (debounce 1 s) |
| Présence | 20 s par joueur (EXPIRE) | via pipeline, pas un message |
| `queue.status` | à chaque changement, ≥ 2 s d'intervalle | |

## 5. Exemple de code

```java
// Émission (proxy) :
bus.request(Channels.ORCHESTRATOR, new QueueJoin(List.of(uuid), partyId, "bedwars", "4x4", null), Duration.ofSeconds(3))
   .thenAccept(reply -> reply.as(QueueJoinReply.class).ifPresent(r -> ...));

// Réception (orchestrateur) :
bus.subscribe(Channels.ORCHESTRATOR, QueueJoin.class, (env, msg) -> queueService.join(msg).toReply());
```
