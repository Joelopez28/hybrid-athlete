---
name: verificacion-fullstack
description: Verificar y depurar la cadena React → Spring Boot → PostgREST → PostgreSQL con evidencia reproducible (builds, tests, smoke, logs).
whenToUse: Antes de cerrar un cambio que toca API, esquema o UI; cuando algo falla y no está claro en qué capa; para preparar un PR.
---

# Verificación full-stack

Regla de oro: **no digas "funciona" sin un comando y su salida**. La cadena tiene cuatro eslabones y
cada uno falla distinto.

## Escalera de verificación (barato → caro)

```bash
# 0) Sintaxis de infra
docker compose config -q

# 1) Contenedores de datos
make up
docker compose ps                       # db healthy, postgrest running
curl -sS -i 'http://localhost:3000/tasks?limit=1'   # 200 + JSON

# 2) Backend
cd backend && ./mvnw test               # unit + slice tests
curl -sS 'http://localhost:8080/actuator/health'    # {"status":"UP"}

# 3) Contrato real (BFF → PostgREST → DB)
make smoke

# 4) Frontend
cd frontend && npm run typecheck && npm run build

# 5) Stack completo (opcional, valida Dockerfiles y Nginx)
docker compose -f compose.full.yaml up --build -d
curl -sS http://localhost:8081/api/tasks
```

`make smoke` debe: crear una tarea, listarla, cambiarle el estado, verificar el total y borrarla,
dejando la base como estaba. Si el smoke falla, mirá qué paso imprimió (`scripts/smoke.sh` usa `set -euo pipefail`).

## Diagnóstico por capa

| Síntoma | Capa | Qué mirar |
|---|---|---|
| `connection refused :3000` | PostgREST | `docker compose ps`, logs; DB no healthy |
| 401/403 en PostgREST | roles/grants | skill `datos-postgrest`; `PGRST_DB_ANON_ROLE` |
| 502 desde Spring | BFF→PostgREST | `POSTGREST_URL`, red del contenedor, timeout |
| 400 con `errors[]` | validación | body del request, constraints del DTO |
| 500 sin log | BFF | `docker compose logs backend` / consola Maven |
| UI en blanco | React | consola del navegador, `vite` logs, error de import |
| CORS en dev | proxy | ¿llamaste a `:8080` directo? Usá rutas relativas `/api` |
| `npm run build` falla | TS | error exacto de `tsc`; no silencies con `any` |
| Docker build backend lento | Maven | cache de capas; `-DskipTests` solo en imagen |

## Pruebas manuales mínimas de la UI

1. Lista carga con seed; filtro por estado cambia la lista.
2. Crear con título vacío → error visible y foco en el campo.
3. Crear válida → aparece arriba, contador actualizado.
4. Cambiar estado → persiste tras recargar (F5).
5. Borrar → pide confirmación y desaparece.
6. Backend apagado → banner de error con reintento, sin pantalla rota.
7. Solo teclado: tabular por formulario, enviar con Enter, foco visible.

## Evidencia para el cierre

```text
✅ docker compose config -q
✅ curl :3000/tasks → 200
✅ ./mvnw test → 12 tests, 0 failures
✅ npm run typecheck && npm run build → ok
✅ make smoke → created/listed/updated/deleted
⚠️ Pendiente: RLS (fuera de alcance)
```

Pegá esto en el mensaje final, junto con archivos tocados. Si algo no se pudo probar (p. ej. Docker
no disponible), decilo explícitamente y explicá qué falta.

## Cuándo NO verificar con Docker

- Solo cambiaste texto/estilos: alcanza typecheck + build.
- Solo tests unitarios: `./mvnw test` sin DB.
- Entorno sin Docker: usá `curl` contra PostgREST si está disponible; si no, unit tests + build y
  aclaralo. Nunca inventes una salida.

## Scripts del repo

| Script | Uso |
|---|---|
| `scripts/smoke.sh` | cadena completa vía API, con cleanup |
| `scripts/dev.sh` | levanta infra y sugiere comandos (no bloquea) |
| `make test` | tests backend + typecheck/build frontend |
| `make smoke` | smoke con mensajes por paso |
