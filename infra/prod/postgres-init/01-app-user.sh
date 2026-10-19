#!/bin/bash
# Crée l'utilisateur applicatif nexus_app (DML uniquement) — exécuté une seule fois à l'initialisation.
# NEXUS_APP_PASSWORD est fourni par docker-compose.data.yml (depuis infra/prod/.env, non versionné).
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE nexus_app LOGIN PASSWORD '${NEXUS_APP_PASSWORD:?}';
GRANT CONNECT ON DATABASE nexus TO nexus_app;
GRANT USAGE ON SCHEMA public TO nexus_app;
ALTER DEFAULT PRIVILEGES FOR ROLE nexus_owner IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO nexus_app;
ALTER DEFAULT PRIVILEGES FOR ROLE nexus_owner IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO nexus_app;
SQL
