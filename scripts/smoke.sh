#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
API_BASE="${API_BASE:-$BACKEND_URL/api}"
POSTGREST_URL="${POSTGREST_URL:-http://localhost:3000}"
TITLE="Smoke RUKY $(date +%s)"
SESSION_ID=""
TOKEN=""

ok() { printf 'OK   %s\n' "$*"; }
fail() { printf 'FAIL %s\n' "$*" >&2; exit 1; }

cleanup() {
  if [[ -n "$SESSION_ID" ]]; then
    curl -fsS -X DELETE -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions/$SESSION_ID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

command -v curl >/dev/null 2>&1 || fail "curl es necesario"

printf '\n== RUKY smoke ==\n'

curl -fsS "$BACKEND_URL/actuator/health" | grep -q '"status":"UP"' \
  || fail "El backend no está UP en $BACKEND_URL"
ok "Backend UP"

login="$(curl -fsS -X POST "$BACKEND_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"ruky@ruky.demo","password":"ruky1234"}')" \
  || fail "No se pudo iniciar sesión demo (¿make db-reset?)"
TOKEN="$(printf '%s' "$login" | grep -o '"token":"[^"]*"' | head -n 1 | cut -d'"' -f4)"
[[ -n "$TOKEN" ]] || fail "El login no devolvió un token"
ok "Login JWT"

curl -fsS -H "Authorization: Bearer $TOKEN" "$POSTGREST_URL/training_sessions?limit=1" >/dev/null \
  || fail "PostgREST no acepta el JWT en $POSTGREST_URL (ejecutá: make up)"
ok "PostgREST + JWT"

before_total="$(curl -fsS -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions?size=1" | grep -o '"total":[0-9]*' | head -n 1 | cut -d: -f2)"
[[ -n "$before_total" ]] || fail "No se pudo leer el total de sesiones"
ok "Sesiones iniciales: $before_total"

created="$(curl -fsS -X POST "$API_BASE/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"performedOn\":\"$(date +%F)\",\"discipline\":\"POWERLIFTING\",\"sessionType\":\"STRENGTH\",\"title\":\"$TITLE\",\"durationMin\":30,\"rpe\":7.5,\"entries\":[{\"movement\":\"Back Squat\",\"setIndex\":1,\"reps\":5,\"weightKg\":100}]}")"

SESSION_ID="$(printf '%s' "$created" | grep -o '"id":"[^"]*"' | head -n 1 | cut -d'"' -f4)"
[[ -n "$SESSION_ID" ]] || fail "La API no devolvió un id al crear la sesión"
printf '%s' "$created" | grep -q '"volumeKg":500' \
  || fail "El volumen calculado no es 500 kg"
ok "Sesión creada con 500 kg de volumen: $SESSION_ID"

curl -fsS -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions?size=100" | grep -q "$SESSION_ID" \
  || fail "La sesión creada no aparece en el historial"
ok "La sesión aparece en el historial"

after_create="$(curl -fsS -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions?size=1" | grep -o '"total":[0-9]*' | head -n 1 | cut -d: -f2)"
[[ "$after_create" == "$((before_total + 1))" ]] \
  || fail "El total no aumentó ($before_total -> $after_create)"
ok "Total actualizado: $after_create"

code="$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions/$SESSION_ID")"
[[ "$code" == "204" ]] || fail "El DELETE devolvió $code (se esperaba 204)"
deleted_id="$SESSION_ID"
SESSION_ID=""
ok "Sesión eliminada (204)"

curl -fsS -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions?size=100" | grep -q "$deleted_id" \
  && fail "La sesión eliminada sigue apareciendo"
ok "La sesión ya no está en el historial"

after_total="$(curl -fsS -H "Authorization: Bearer $TOKEN" "$API_BASE/sessions?size=1" | grep -o '"total":[0-9]*' | head -n 1 | cut -d: -f2)"
[[ "$before_total" == "$after_total" ]] \
  || fail "El total no volvió al valor inicial ($before_total -> $after_total)"
ok "Total restaurado: $after_total"

printf '\nSmoke completo: la cadena UI -> API -> PostgREST -> PostgreSQL funciona.\n'
