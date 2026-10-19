# Glossaire

| Terme | Définition |
|---|---|
| **Mode** (`ModeId`) | Un mini-jeu : `dropper`, `rush`, `bedwars`. |
| **Variante** (`VariantId`) | Format d'un mode : nombre d'équipes × taille (`8x1`, `4x4`). En BDD (`game_variants`). |
| **Preset** | Jeu de valeurs de config nommé et versionné d'un mode (`classic`, `fast`). |
| **Instance** | Une partie en cours sur un serveur de jeu, avec son propre monde. Id `bw-7f3a`. |
| **Match** | L'enregistrement en BDD d'une instance jouée (`matches`), id UUIDv7. |
| **Phase** | Étape de la vie d'une instance (Waiting, Countdown, InGame, End…). |
| **Module** | Comportement réutilisable activable par jeu ou par phase (`GeneratorModule`). |
| **Marqueur** | Point/bloc/région placé dans l'éditeur de maps, défini par le `MapSchema`. |
| **Template** | Archive d'un monde publié, copiée pour chaque instance. |
| **Profil serveur** | Type d'un serveur Paper : `lobby`, `game`, `editor`. |
| **Orchestrateur** | Service qui gère files, instances et serveurs. Un leader actif. |
| **Réservation** | Place promise à un joueur dans une instance (TTL 15 s) avant sa connexion. |
| **Classé (ranked)** | Match qui alimente les stats agrégées. Les parties privées/forcées ne le sont pas. |
| **Final kill** | Kill d'un joueur qui ne peut plus réapparaître (lit détruit). |
| **Mort subite** | Phase de fin de partie accélérée (lits détruits, bordure, etc.). |
| **Hub admin** | Menu principal d'administration en jeu (`/admin`). |
| **MSPT** | Millisecondes par tick (budget 50 ms à 20 TPS). |
