---
name: datos-postgrest
description: Modelar PostgreSQL y exponerlo con PostgREST para RUKY: atletas, ejercicios, sesiones, series, RPC de creación y roles.
whenToUse: Al cambiar el esquema, el seed, la función create_session, roles/RLS o depurar consultas PostgREST.
---

# Datos: PostgreSQL + PostgREST (RUKY)

## Tablas (esquema `api`)

```text
athletes ──< training_sessions ──< session_entries >── exercises
```

- `athletes`: handle único, sexo, peso, `tier (OPEN|ELITE)`, disciplinas (`text[]`).
- `exercises`: catálogo compartido, `discipline`, `category`, `unit`, `benchmark`.
- `training_sessions`: atleta, fecha, disciplina, tipo, duración, RPE, resultado WOD/HYROX.
- `session_entries`: sesión, ejercicio opcional, `movement` denormalizado, `set_index`, `reps`,
  `weight_kg`, `seconds`, `rpe`.
- Competencias: `competitions` → `competition_divisions` / `competition_events`;
  `competition_registrations` (única por competencia/atleta) y `competition_results`
  (única por evento/atleta). Las FK compuestas garantizan que división/evento pertenezcan a la
  competencia.

Convenciones:
- snake_case en DB; el mapper del BFF lo traduce a camelCase.
- `uuid` + `gen_random_uuid()`; `timestamptz`; `timestamptz` trigger para `updated_at`.
- Constraints en la base **además** de validación Java.
- Índices: `(athlete_id, performed_on desc)`, `(session_id, set_index)`, `(discipline, name)`.

## Crear sesión con series: RPC

PostgREST 12 de este deployment no acepta nested inserts. Se usa:

```http
POST /rpc/create_session
{"payload": {
  "athlete_id": "...", "performed_on": "2026-10-20",
  "discipline": "POWERLIFTING", "session_type": "STRENGTH",
  "title": "Fuerza", "entries": [
    {"exercise_id": "...", "movement": "Back Squat", "set_index": 1,
     "reps": 5, "weight_kg": 100}
  ]
}}
```

La función inserta sesión + series en una transacción y devuelve el `uuid`. Reglas:
- `nullif(...)` para campos opcionales; `coalesce(...,1)` para `set_index`.
- `grant execute` a `web_anon` (dev).
- Tras crear la función: `NOTIFY pgrst, 'reload schema';` (ya está al final de `005_functions.sql`).
- PostgREST necesita el wrapper `payload`; documentado en el backend.

## Competencias: upsert

```http
POST /competition_registrations?on_conflict=competition_id,athlete_id&select=*,athlete:athletes(*)
Prefer: resolution=merge-duplicates,return=representation
{"competition_id":"...","division_id":"...","athlete_id":"...","status":"REGISTERED"}

POST /competition_results?on_conflict=event_id,athlete_id
Prefer: resolution=merge-duplicates,return=representation
{"competition_id":"...","event_id":"...","athlete_id":"...","raw_score":240,"display_score":"04:00"}
```

- El puntaje no se guarda: se calcula en `CompetitionScoring`.
- El seed (`007_competition_demo.sql`) crea 1 competencia LIVE, 2 divisiones, 4 pruebas y 16 resultados.
- Para cerrar una competencia, actualizá `status` a `FINISHED` (admin es fase 2).

## Consultas que usa el BFF

| Necesidad | Query |
|---|---|
| Sesiones con series | `/training_sessions?select=*,session_entries(*)&athlete_id=eq.{id}` |
| Orden | `order=performed_on.desc,created_at.desc` |
| Conteo exacto | `Prefer: count=exact` → `Content-Range: 0-19/42` |
| Filtros | `discipline=eq.CROSSFIT`, `session_type=eq.WOD` |
| Stats | `performed_on=gte.2026-09-01&limit=500` |
| Leaderboard | `performed_on=gte.…&limit=2000` (todas las sesiones) |
| Catálogo | `/exercises?order=discipline.asc,name.asc` |

Las agregaciones (volumen, PRs, ranking) se hacen en Java. Si el volumen crece, moverlas a vistas
SQL y consultarlas por PostgREST.

## Roles y RLS

- `authenticator` (login, `NOINHERIT`), `web_anon` (dev), `grant web_anon to authenticator`.
- Producción: `app_user` + JWT + RLS por `owner_id`/`sub`; `web_anon` desaparece.
- El owner de la tabla no debe saltarse RLS en producción (usar `FORCE ROW LEVEL SECURITY`).

## Seed y reset

- `002_schema.sql`, `003_exercises.sql`, `004_demo_data.sql`, `005_functions.sql` corren **solo** al
  crear el volumen.
- El seed usa `current_date` para que el dashboard tenga actividad de la semana actual.
- `make db-reset` destruye el volumen; nunca en producción.
- Un cambio nuevo va en `006_*.sql`; no edites un script ya aplicado.

## Próxima fase: competencias

Cuando se implemente Strong Fit, agregar:

```text
competitions (id, name, starts_on, ends_on, status)
competition_events (id, competition_id, name, scoring, order)
registrations (competition_id, athlete_id, division, status)
event_results (event_id, athlete_id, raw_score, points, validated)
```

El puntaje por posición y los desempates van en backend/SQL, no en la UI. No mezclar con
`LeaderboardService`.

## Depuración

```bash
docker compose logs -f postgrest
docker compose exec db psql -U ruky -d ruky -c '\dt api.*'
docker compose exec db psql -U ruky -d ruky -c 'select * from api.training_sessions limit 3;'
curl -s 'http://localhost:3000/training_sessions?select=*,session_entries(*)&limit=1'
```

| Síntoma | Causa |
|---|---|
| `PGRST204 Could not find 'session_entries' column` | intento de nested insert; usá la RPC |
| `PGRST202 function does not exist` | falta reload o body sin `payload` |
| 401/403 | roles/grants o volumen viejo |
| columnas nuevas no aparecen | `NOTIFY pgrst, 'reload schema'` |
| fechas corridas | `timestamptz` vs `date`; respetá el tipo de columna |

## Checklist

- [ ] DDL nuevo numerado y probado desde volumen limpio.
- [ ] Grants mínimos; RLS antes de producción.
- [ ] RPC/grant/reload actualizados si cambia la firma.
- [ ] `curl` directo documentado para la query nueva.
