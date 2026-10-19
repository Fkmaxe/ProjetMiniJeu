#!/usr/bin/env bash
# Tests du hook commit-msg : messages acceptés et refusés (lancé par ./gradlew testGitHooks).
set -euo pipefail

readonly HOOK="$(cd "$(dirname "$0")/.." && pwd)/commit-msg"
work="$(mktemp -d)"
trap 'rm -rf "${work}"' EXIT
failures=0

check() {
  local expected="$1" label="$2" message="$3"
  printf '%s\n' "${message}" > "${work}/msg"
  if "${HOOK}" "${work}/msg" 2>/dev/null; then actual=accepte; else actual=refuse; fi
  if [[ "${actual}" != "${expected}" ]]; then
    echo "ÉCHEC : ${label} → ${actual} (attendu : ${expected})" >&2
    failures=$((failures + 1))
  fi
}

check accepte "feat avec Refs" $'feat(core): libère la réservation\n\nRefs: P2-06'
check accepte "plusieurs scopes" $'feat(lobby,proxy): ajoute les points d\'entrée\n\nRefs: P0-04'
check accepte "docs sans Refs" 'docs: spécifications et règles de développement'
check accepte "chore de fusion" 'chore: fusionne feat/P0-01-monorepo-gradle'
check accepte "commit de fusion Git" "Merge branch 'feat/x'"
check accepte "changement cassant" $'feat(api)!: renomme GameContext\n\nBREAKING CHANGE: …\n\nRefs: P3-01'
check accepte "72 caractères accentués" "docs: $(printf 'é%.0s' $(seq 1 66))"
check refuse "Co-Authored-By" $'feat(core): ajoute X\n\nRefs: P1-05\nCo-Authored-By: Someone <noreply@anthropic.com>'
check refuse "co-authored-by minuscule" $'docs: met à jour\n\nco-authored-by: a <a@b.c>'
check refuse "lien de session" $'docs: met à jour\n\nClaude-Session: https://claude.ai/code/session_x'
check refuse "Generated with" $'docs: met à jour\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)'
check refuse "type inconnu" 'feature: ajoute X'
check refuse "scope inconnu" $'feat(web): ajoute X\n\nRefs: P1-01'
check refuse "point final" 'docs: met à jour la doc.'
check refuse "73 caractères" "docs: $(printf 'é%.0s' $(seq 1 67))"
check refuse "feat sans Refs" 'feat(core): ajoute X'
check refuse "test sans Refs" 'test(core): vérifie X'

if (( failures > 0 )); then
  echo "${failures} cas en échec" >&2
  exit 1
fi
echo "commit-msg : tous les cas passent"
