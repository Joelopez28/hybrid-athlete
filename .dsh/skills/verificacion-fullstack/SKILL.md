---
name: verificacion-fullstack
description: Verificar y depurar RUKY de punta a punta: tests, build, smoke de sesiones, UI E2E, mobile y stack Docker.
whenToUse: Antes de cerrar cambios de API, datos o UI; cuando algo falla y no se sabe en qué capa; para preparar un PR.
---

# Verificación full-stack (RUKY)

Regla de oro: **no digas “funciona” sin comando y salida**. La cadena tiene cuatro eslabones y cada
uno falla distinto.

## Escalera

```bash
# 0) Infra
docker compose config -q
make up
docker compose ps                      # db healthy, postgrest up
curl -sS -i 'http://localhost:3000/training_sessions?limit=1'

# 1) Backend
cd backend && ./mvnw test              # 47 tests
curl -sS http://localhost:8080/actuator/health

# 2) Contrato real (crea, verifica volumen, lista, borra)
make smoke

# 3) Frontend
cd frontend && npm run typecheck && npm run build

# 4) Stack completo
make full-up                           # http://localhost:8081
curl -sS http://localhost:8081/api/stats?weeks=4
```

## Smoke esperado

```text
OK   PostgREST responde
OK   Backend UP
OK   Sesiones iniciales: 10
OK   Sesión creada con 500 kg de volumen: <uuid>
OK   La sesión aparece en el historial
OK   Total actualizado: 11
OK   Sesión eliminada (204)
OK   La sesión ya no está en el historial
OK   Total restaurado: 10
```

Si falla:
- PostgREST no responde → contenedor/roles.
- 400 al crear → payload/validación.
- El volumen no es 500 → cálculo/entradas.
- 502 → RPC sin `payload`, función sin reload o PostgREST caído.

## Recorrido de UI (manual o Playwright)

0. Login con `ruky@ruky.demo` / `ruky1234`; sin token, la app muestra Auth.
1. Dashboard: saludo, 4 stat cards, chart, sesiones recientes, mini ranking.
2. Registrar: elegir disciplina → cambia tipo/catálogo; completar título y series; guardar.
3. Historial: la sesión nueva aparece arriba con volumen correcto; expandir series.
4. Borrar: confirmar, desaparece, total baja.
5. Progreso: PRs (kg/reps/segundos), chart y mezcla.
6. Competir: inscribirse/cambiar división, cargar un resultado, ver la marca formateada y el
   leaderboard de la división propia; la tabla scrollea y el Total queda fijo.
7. Comunidad: cambiar métrica/período/división; Ruky Elite filtra solo `ELITE`.
8. Logout y re-login; la sesión persiste. Registro nuevo crea atleta OPEN.
9. Mobile 390px: navegación envuelta, sin scroll horizontal, tablas con scroll interno.

## Diagnóstico por capa

| Síntoma | Capa | Qué mirar |
|---|---|---|
| `connection refused :3000` | PostgREST | `docker compose ps`, logs, DB healthy |
| 401/403 | roles/grants | skill `datos-postgrest` |
| 502 al crear | BFF→RPC | `payload`, función, reload schema |
| 400 `entries` | validación | tipo de sesión, reps/segundos/score |
| PR muestra “0 reps” | stats/UI | `bestSeconds` / `bestReps` |
| Chart vacío | fechas/seed | ventana de semanas, `current_date` |
| Ranking vacío | filtros | período, tier, sesiones en rango |
| UI rompe en móvil | CSS | `scrollWidth - innerWidth`, fieldset `min-width` |
| CORS 403 | dev | origen en `CORS_ALLOWED_ORIGINS` |

## Evidencia para el cierre

```text
✅ docker compose config -q
✅ curl :3000/training_sessions → 200
✅ ./mvnw test → 47 tests, 0 failures
✅ Auth E2E → login, registro, RLS, logout, re-login
✅ npm run typecheck && npm run build → ok
✅ make smoke → created/listed/deleted, total restaurado
✅ UI E2E → dashboard, crear, historial, borrar, progreso, Elite, mobile 0 overflow
```

Si algo no se pudo probar (Docker apagado, sin navegador), decilo con el motivo. Nunca inventes una
salida.

## Limpieza

- `make full-down` / `make down` al terminar pruebas pesadas.
- `make db-reset` deja datos demo reproducibles.
- No borres volúmenes de un entorno que no sea tuyo.
