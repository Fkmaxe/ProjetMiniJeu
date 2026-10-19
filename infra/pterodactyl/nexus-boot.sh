#!/bin/bash
# =====================================================================================
# nexus-boot.sh — script de démarrage commun à tous les serveurs Nexus sous Pterodactyl.
# Lancé par la commande de démarrage des eggs (« bash ./nexus-boot.sh »), dans /home/container.
#   1. synchronise les jars Nexus depuis l'archive de release du rôle (si NEXUS_RELEASE_URL est défini)
#   2. pose les configs de référence absentes et injecte les secrets
#   3. lance la JVM du rôle (exec → Pterodactyl reçoit directement stdin/stdout et les signaux)
# Documentation : docs/17-deploiement-pterodactyl.md — toute modification ici doit y être reportée.
# =====================================================================================
set -euo pipefail

ROLE="${NEXUS_ROLE:?NEXUS_ROLE manquant (proxy|lobby|game|editor|orchestrator)}"
VERSION="${NEXUS_VERSION:-latest}"
RELEASE_URL="${NEXUS_RELEASE_URL:-}"
UA="nexus-boot/1.0 (${NEXUS_CONTACT:-https://example.invalid})"
STATE_DIR=".nexus"
mkdir -p "${STATE_DIR}"

log() { echo "[nexus-boot] $*"; }

case "${ROLE}" in
  proxy)              BUNDLE="proxy";        TARGET_DIR="plugins" ;;
  lobby|game|editor)  BUNDLE="${ROLE}";      TARGET_DIR="plugins" ;;
  orchestrator)       BUNDLE="orchestrator"; TARGET_DIR="app" ;;
  *) log "rôle inconnu : ${ROLE}"; exit 1 ;;
esac
mkdir -p "${TARGET_DIR}"

# ---------------------------------------------------------------------------
# 1. Synchronisation de l'archive de release
#    ${RELEASE_URL}/${VERSION}/nexus-${BUNDLE}.tar.gz (+ .sha256)
#    Contenu de l'archive : jars/ (jars Nexus), config/ (configs de référence), VERSION
# ---------------------------------------------------------------------------
sync_bundle() {
  local base="${RELEASE_URL%/}/${VERSION}/nexus-${BUNDLE}.tar.gz"
  local remote_sum local_sum
  remote_sum="$(curl -fsSL -A "${UA}" "${base}.sha256" | awk '{print $1}')"
  local_sum="$(cat "${STATE_DIR}/bundle.sha256" 2>/dev/null || true)"
  if [[ "${remote_sum}" == "${local_sum}" ]]; then
    log "archive ${BUNDLE}@${VERSION} déjà à jour"
    return
  fi
  log "téléchargement de ${BUNDLE}@${VERSION}"
  curl -fsSL -A "${UA}" -o "${STATE_DIR}/bundle.tar.gz" "${base}"
  echo "${remote_sum}  ${STATE_DIR}/bundle.tar.gz" | sha256sum -c --quiet -
  rm -rf "${STATE_DIR}/staging" && mkdir -p "${STATE_DIR}/staging"
  tar -xzf "${STATE_DIR}/bundle.tar.gz" -C "${STATE_DIR}/staging"
  # On ne supprime QUE les jars Nexus (préfixe Nexus / nexus-), jamais les plugins tiers (spark…)
  find "${TARGET_DIR}" -maxdepth 1 -type f \( -name 'Nexus*.jar' -o -name 'nexus-*.jar' \) -delete
  cp "${STATE_DIR}"/staging/jars/*.jar "${TARGET_DIR}/"
  # Configs de référence : copiées uniquement si absentes (les réglages locaux ne sont jamais écrasés)
  if [[ -d "${STATE_DIR}/staging/config" ]]; then
    cp -rn "${STATE_DIR}"/staging/config/. ./ 2>/dev/null || true
  fi
  echo "${remote_sum}" > "${STATE_DIR}/bundle.sha256"
  log "installé : $(cat "${STATE_DIR}/staging/VERSION" 2>/dev/null || echo "${VERSION}")"
}

if [[ -n "${RELEASE_URL}" ]]; then
  sync_bundle || { log "échec de synchronisation — démarrage avec les jars présents"; }
else
  log "NEXUS_RELEASE_URL vide : mode manuel (jars déposés par SFTP)"
fi

# ---------------------------------------------------------------------------
# 2. Secrets & réglages dépendant de l'environnement
# ---------------------------------------------------------------------------
if [[ "${ROLE}" == "proxy" ]]; then
  printf '%s' "${VELOCITY_SECRET:?VELOCITY_SECRET manquant}" > forwarding.secret
elif [[ "${ROLE}" != "orchestrator" && -f config/paper-global.yml ]]; then
  # Paper n'interpole pas les variables d'environnement : on remplace la ligne « secret: » de la section velocity
  sed -i -E "s|^(\s*secret:).*|\1 '${VELOCITY_SECRET:?VELOCITY_SECRET manquant}'|" config/paper-global.yml
fi

# Adresse annoncée à l'orchestrateur (Velocity s'y connecte) : IP privée de l'allocation Pterodactyl
export NEXUS_ADVERTISED_HOST="${NEXUS_ADVERTISED_HOST:-${SERVER_IP:-127.0.0.1}}"
export NEXUS_ADVERTISED_PORT="${NEXUS_ADVERTISED_PORT:-${SERVER_PORT:-25565}}"
export NEXUS_SERVER_TYPE="${ROLE}"

# ---------------------------------------------------------------------------
# 3. JVM
#    SERVER_MEMORY (Mo) est fourni par Pterodactyl ; on laisse ~10 % au hors-tas.
# ---------------------------------------------------------------------------
HEAP_MB=$(( ${SERVER_MEMORY:-2048} * 90 / 100 ))
COMMON_FLAGS="-Xms${HEAP_MB}M -Xmx${HEAP_MB}M -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch -XX:+PerfDisableSharedMem -Dfile.encoding=UTF-8"
EXTRA_FLAGS="${JVM_FLAGS:-}"

case "${ROLE}" in
  proxy)
    exec java ${COMMON_FLAGS} -XX:+UseG1GC ${EXTRA_FLAGS} -jar "${SERVER_JARFILE:-velocity.jar}"
    ;;
  orchestrator)
    exec java ${COMMON_FLAGS} -XX:+UseZGC ${EXTRA_FLAGS} -jar app/nexus-orchestrator.jar
    ;;
  *)
    # GC : ZGC par défaut ; à confirmer par le test de charge P11-03 (docs/12-performance.md §5)
    exec java ${COMMON_FLAGS} -XX:+UseZGC ${EXTRA_FLAGS} -jar "${SERVER_JARFILE:-server.jar}" --nogui
    ;;
esac
