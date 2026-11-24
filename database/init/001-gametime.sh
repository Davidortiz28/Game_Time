#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=app_password="$GAMETIME_APP_PASSWORD" <<'SQL'
CREATE EXTENSION IF NOT EXISTS vector;
CREATE ROLE gametime_app WITH LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE gametime TO gametime_app;
GRANT USAGE, CREATE ON SCHEMA public TO gametime_app;
SQL
