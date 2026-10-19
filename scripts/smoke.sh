#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
API_BASE="${API_BASE:-$BACKEND_URL/api}"
POSTGREST_URL="${POSTGREST_URL:-http://localhost:3000}"
TASK_ID=""

ok() { printf 'OK   %s\n' "$*"; }
fail() { printf 'FAIL %s\n' "$*" >&2; exit 1; }

cleanup() {
  if [[ -n "$TASK_ID" ]]; then
    curl -fsS -X DELETE "$API_BASE/tasks/$TASK_ID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

command -v curl >/dev/null 2>&1 || fail "curl es necesario"

printf '\n== TaskFlow smoke ==\n'

curl -fsS "$POSTGREST_URL/tasks?limit=1" >/dev/null \
  || fail "PostgREST no responde en $POSTGREST_URL (ejecutá: make up)"
ok "PostgREST responde"

curl -fsS "$BACKEND_URL/actuator/health" | grep -q '"status":"UP"' \
  || fail "El backend no está UP en $BACKEND_URL"
ok "Backend UP"

before_total="$(curl -fsS "$API_BASE/tasks?size=1" | grep -o '"total":[0-9]*' | head -n 1 | cut -d: -f2)"
[[ -n "$before_total" ]] || fail "No se pudo leer el total de tareas"
ok "Total inicial: $before_total"

created="$(curl -fsS -X POST "$API_BASE/tasks" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Smoke test automatizado","description":"Creada por scripts/smoke.sh","priority":"HIGH","dueDate":"2030-12-31"}')"

TASK_ID="$(printf '%s' "$created" | grep -o '"id":"[^"]*"' | head -n 1 | cut -d'"' -f4)"
[[ -n "$TASK_ID" ]] || fail "La API no devolvió un id al crear la tarea"
ok "Tarea creada: $TASK_ID"

curl -fsS "$API_BASE/tasks?size=100" | grep -q "$TASK_ID" \
  || fail "La tarea creada no aparece en el listado"
ok "La tarea aparece en el listado"

patched="$(curl -fsS -X PATCH "$API_BASE/tasks/$TASK_ID" \
  -H 'Content-Type: application/json' \
  -d '{"status":"DONE"}')"
printf '%s' "$patched" | grep -q '"status":"DONE"' \
  || fail "El PATCH no actualizó el estado"
ok "Estado actualizado a DONE"

deleted_id="$TASK_ID"
code="$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$API_BASE/tasks/$TASK_ID")"
[[ "$code" == "204" ]] || fail "El DELETE devolvió $code (se esperaba 204)"
TASK_ID=""
ok "Tarea eliminada (204)"

curl -fsS "$API_BASE/tasks?size=100" | grep -q "$deleted_id" \
  && fail "La tarea eliminada sigue apareciendo"
ok "La tarea ya no está en el listado"

after_total="$(curl -fsS "$API_BASE/tasks?size=1" | grep -o '"total":[0-9]*' | head -n 1 | cut -d: -f2)"
[[ "$before_total" == "$after_total" ]] \
  || fail "El total cambió ($before_total -> $after_total)"
ok "Total restaurado: $after_total"

printf '\nSmoke completo: la cadena API -> PostgREST -> PostgreSQL funciona.\n'
