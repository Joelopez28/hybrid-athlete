# RUKY — instrucciones del workspace

Aplicación de entrenamiento y competencia para **CrossFit, powerlifting, calistenia y HYROX**:
registro de sesiones, progreso, PRs, comunidad y división **Ruky Elite**.
Todo el equipo humano y los agentes de DSH siguen este archivo. Es la fuente de verdad del repo.

## Mapa del repo

```text
backend/        Spring Boot 3.3 · Java 17 · BFF y cálculos (paquete com.ruky.training)
frontend/       React 19 + TypeScript + Vite · UI de 5 secciones
db/init/        Roles, esquema, catálogo de ejercicios, demo y RPC (PostgreSQL)
compose.yaml    Postgres + PostgREST para desarrollo
compose.full.yaml  Stack completo en contenedores (backend + frontend)
scripts/        smoke.sh, dev.sh
.dsh/skills/    Skills reutilizables del equipo de agentes
.dsh/agents/    Briefs de subagentes (backend, frontend, datos, reviewer, integrador)
docs/           Capturas y decisiones (ADR)
```

## Arquitectura (no improvisar esta frontera)

```text
React (5173) --/api--> Spring Boot (8080) --HTTP--> PostgREST (3000) --> PostgreSQL (5432)
```

- El **navegador nunca llama a PostgREST**. PostgREST queda en la red privada; Spring Boot es el único cliente.
- **PostgreSQL es la fuente de verdad**; el esquema vive en `db/init/` y se expone en el esquema `api`.
- **Spring Boot** aporta contrato estable `/api`, validación, errores, agregaciones y el lugar donde entrará auth.
- **PostgREST** resuelve lecturas y escrituras declarativas. La creación de sesiones con series usa la función
  atómica `api.create_session(jsonb)` vía RPC (PostgREST 12 no soporta nested inserts en este deployment).
- **React** solo conoce los tipos de `frontend/src/api/types.ts`; nunca arma URLs de PostgREST.

## Comandos

```bash
make up            # Postgres + PostgREST
make install       # deps frontend (una vez)
make backend       # Spring Boot en :8080
make frontend      # Vite en :5173
make test          # 23 tests backend + typecheck/build frontend
make smoke         # crea sesión, verifica volumen, lista y borra
make full-up       # stack completo en Docker (:8081)
make db-reset      # borra el volumen y re-siembra (destructivo)
```

Detalle y troubleshooting en `README.md`.

## Producto (MVP actual)

- **Modo demo sin auth**: todas las escrituras pertenecen al atleta configurado en
  `DEMO_ATHLETE_HANDLE` (default `ruky`). No hay login todavía; no simularlo.
- **Sesiones**: fuerza, WOD (AMRAP/EMOM/For Time/Intervalos), simulación HYROX, skills, cardio y mixto.
- **Series**: ejercicio del catálogo, repeticiones, peso (kg), segundos (holds) y RPE.
- **Progreso**: volumen semanal, PRs, minutos, mezcla de disciplinas.
- **Comunidad**: ranking por volumen, sesiones o minutos; filtro por período y división (`OPEN` / `ELITE`).
- **Ruky Elite**: división de élite del atleta (`athletes.tier`). Los estándares que muestra la UI son
  **referencia**, no un sistema de clasificación implementado.

## Contrato de la API

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/me` | Atleta demo actual |
| `GET` | `/api/athletes` | Comunidad |
| `GET` | `/api/exercises?discipline=` | Catálogo |
| `GET` | `/api/sessions?discipline=&type=&page=&size=` | Historial paginado con series |
| `GET` | `/api/sessions/{id}` | Detalle |
| `POST` | `/api/sessions` | Crea sesión + series (201) |
| `DELETE` | `/api/sessions/{id}` | Borra (204/404) |
| `GET` | `/api/stats?weeks=8` | Totales, semanal, PRs, disciplinas |
| `GET` | `/api/leaderboard?weeks=4&metric=VOLUME&tier=ELITE` | Ranking |

- JSON camelCase; fechas ISO-8601; errores `{ timestamp, status, error, message, path }`.
- Códigos: 400 validación, 404 no existe, 409 conflicto, 502 PostgREST caído.
- Cambiar el contrato exige actualizar backend, `frontend/src/api/types.ts`, tests y README.

## Reglas de datos

- Esquema `api`: `athletes`, `exercises`, `training_sessions`, `session_entries`.
- IDs `uuid` generados por la base; el cliente nunca los inventa.
- `created_at`/`updated_at` con `timestamptz`; trigger para `updated_at`.
- **Volumen** = Σ `reps × weight_kg`; las series de peso corporal no suman volumen.
- **PRs** solo de sesiones `STRENGTH` y `SKILL`. Los WOD/HYROX guardan resultados agregados y no
  generan PRs. Holds usan `seconds` como marca.
- La RPC `api.create_session` inserta sesión + series en una transacción. Si cambiás su firma,
  actualizá `SessionWrite`, `SessionRepository` y `005_functions.sql`.
- Cambios de esquema: archivo nuevo numerado (`006_*.sql`); no edites uno ya aplicado en producción.
- `web_anon` es **solo desarrollo**. Producción exige `app_user` + JWT + RLS.

## Calidad obligatoria antes de cerrar

1. `cd backend && ./mvnw test` (o `make test`) verde.
2. `cd frontend && npm run typecheck && npm run build` verde.
3. `make smoke` verde si tocaste API, datos o UI.
4. Recorrido manual/E2E: registrar sesión, verla en historial, borrarla, ver PR/ranking.
5. Sin secretos reales; usar `.env` (ignorado) y `.env.example`.
6. Accesibilidad: labels, foco visible, contraste AA, teclado.

## Convenciones

- **Inglés** para identificadores, archivos, commits y comentarios técnicos.
- **Español** para textos de UI, README y documentación de producto.
- Java: records, constructor injection, sin Lombok, sin JPA (el acceso es HTTP).
- TypeScript: `strict`, sin `any`, tipos espejo del contrato.
- CSS plano con tokens; estética oscura + acento lima. No agregar UI kit sin ADR.
- Commits: `feat|fix|refactor|docs|test|chore(scope): mensaje` en imperativo.

## Cómo trabajamos con agentes (DSH)

1. Leé este archivo al arrancar (se carga solo) y elegí skill según la tarea.
2. **Un dueño por área**: backend, frontend, datos/infra. Reviewer no edita.
3. Si cambia el contrato, actualizá `frontend/src/api/types.ts` y avisá al dueño.
4. Delegación con los briefs de `.dsh/agents/`; reporte obligatorio con comandos y resultado.
5. Cerrá con evidencia, no con “debería funcionar”.

| Skill | Cuándo |
|---|---|
| `arquitectura-fullstack` | diseñar un recurso, cambiar contrato o frontera |
| `backend-spring-boot` | endpoints, RPC, stats, tests Java |
| `frontend-react-ts` | páginas, formularios, estados, estilos |
| `datos-postgrest` | esquema, RPC, roles, consultas, migraciones |
| `verificacion-fullstack` | smoke, E2E, diagnóstico por capa |
| `revision-codigo` | review P0/P1/P2 antes de cerrar |

## Deuda técnica / roadmap

- **Auth real + RLS** por atleta; hoy todo pertenece al atleta demo.
- **Competencias Strong Fit**: tablas `competitions`, `events`, `registrations`, `results`; motor de
  puntaje por posición y desempates. Hoy solo hay teaser en UI.
- **Criterios Ruky Elite**: benchmarks oficiales y flujo de postulación/clasificación.
- Migraciones Flyway/Liquibase; hoy init scripts de Docker.
- E2E Playwright versionado en el repo y CI.
- Paginación/caché del leaderboard; hoy recalcula en memoria sobre ≤2000 sesiones.

Registrá decisiones nuevas en `docs/decisiones.md` con fecha, contexto, decisión y consecuencias.
