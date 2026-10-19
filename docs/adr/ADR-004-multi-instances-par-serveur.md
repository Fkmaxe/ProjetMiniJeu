# ADR-004 — Plusieurs instances par serveur, un monde par instance

- **Statut** : accepté (stratégie de chargement : à confirmer par le spike S4)
- **Date** : 2026-10-19

## Décision
Un serveur de jeu héberge N instances (bornées par `maxInstances` et le `capacityScore`), chacune dans son **propre monde** créé depuis un template et jamais sauvegardé.

## Raisons
- Une JVM par partie gaspille énormément de mémoire (≈ 1 Go de base par JVM).
- Isoler par monde rend le routage d'événements simple et sûr (index `World → Instance`).
- Les maps de mini-jeux sont petites : la copie de quelques fichiers région est rapide.

## Stratégie de monde (`WorldProvider`)
- **Par défaut** : cache local de templates + copie vers tmpfs + `WorldCreator` void + préchargement async (cf. `12-performance.md` §3).
- **Option** : format slime en mémoire (Advanced Slime Paper) si disponible et stable en 26.1 — décidé par le spike S4, même interface.

## Conséquences
- Tout l'état de jeu est scopé à l'instance (règle n°7 du CLAUDE.md).
- Le budget MSPT est partagé : l'orchestrateur doit tenir compte de la charge réelle.
