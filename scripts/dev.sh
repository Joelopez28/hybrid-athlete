#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose up -d --wait db postgrest

cat <<'EOF'

Infra lista:
  PostgreSQL  localhost:5432  (ruky / ruky)
  PostgREST   http://localhost:3000

Ahora, en dos terminales:
  make backend     # http://localhost:8080
  make frontend    # http://localhost:5173

Verificación: make test && make smoke
EOF
