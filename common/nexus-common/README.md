# nexus-common

Java pur, **aucune dépendance** projet ni Minecraft.

- Identifiants (records value-object : `PlayerId`, `ServerId`, `InstanceId`, `ModeId`, `VariantId`, `MatchId`), `Result<T,E>`, UUIDv7.
- Protocole Redis : `Envelope` + un record par message de `docs/04-messaging-protocol.md`.
- `PermissionResolver` / `PermissionTree` (résolution de `docs/08-permissions-roles.md` §2) et catalogue `Permissions`.
- JSON (Gson configuré).

Tâches : P1-01, P1-02.
