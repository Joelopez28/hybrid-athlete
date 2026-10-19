# TaskFlow

Starter full-stack listo para correr: **Spring Boot 3 (BFF) + React 19 + TypeScript + PostgREST + PostgreSQL**, con un **equipo de agentes de programación** para DeepSeek Harness.

![TaskFlow en escritorio](docs/captura-desktop.png)

<sub>También verificada en móvil: [captura responsive](docs/captura-mobile.png).</sub>

## Qué incluye

- **Backend Spring Boot 3.3 / Java 17**: contrato `/api` estable, validación, manejo uniforme de errores, CORS, Actuator y tests (23 verdes).
- **PostgREST 12**: CRUD declarativo sobre PostgreSQL, aislado en la red privada; el navegador nunca lo toca.
- **Frontend React 19 + TypeScript + Vite 6**: cliente tipado, hook de datos con `AbortController`, filtros, paginación, formulario accesible, estados de carga/vacío/error y tema claro/oscuro.
- **PostgreSQL 16**: esquema `api`, roles `authenticator`/`web_anon`, trigger de `updated_at`, seed de 4 tareas.
- **Docker Compose** para desarrollo (`compose.yaml`) y para el stack completo (`compose.full.yaml`, Nginx incluido).
- **Agentes**: 1 `AGENTS.md`, 6 skills y 5 briefs de subagentes en `.dsh/` (ver [Agentes](#agentes-de-programación)).
- **Verificación real**: `make test`, `make smoke` y un recorrido E2E de la UI (crear → filtrar → cambiar estado → borrar) ejecutado durante el desarrollo.

## Arquitectura

```text
┌─────────────────┐      /api       ┌──────────────────┐      HTTP       ┌────────────┐      ┌────────────┐
│ React 19 + TS   │ ──────────────▶ │ Spring Boot BFF  │ ──────────────▶ │ PostgREST  │ ───▶ │ PostgreSQL │
│ :5173 / :8081   │                 │ :8080            │                 │ :3000      │      │ :5432      │
└─────────────────┘                 └──────────────────┘                 └────────────┘      └────────────┘
        UI                          contrato, reglas                    REST declarativo      fuente de verdad
```

- El navegador **solo** conoce `/api`; PostgREST queda en la red privada.
- Spring aporta validación, DTOs estables, errores y el lugar donde crecerán auth y reglas.
- PostgREST evita reescribir CRUD y filtros en Java.
- Las decisiones y sus consecuencias están en [`docs/decisiones.md`](docs/decisiones.md).

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| Docker + Compose | reciente | para PostgreSQL y PostgREST |
| Java | 17+ | el proyecto compila con 17 |
| Node.js | 20+ | Vite 6 |
| Maven | — | **no hace falta**: incluimos `backend/mvnw` |

## Arranque rápido (3 terminales)

```bash
# 0) Opcional: variables locales
cp .env.example .env

# 1) Base de datos + PostgREST (espera a que estén healthy)
make up

# 2) Dependencias del frontend (una sola vez)
make install

# 3) Terminal A — backend en http://localhost:8080
make backend

# 4) Terminal B — frontend en http://localhost:5173
make frontend
```

Abrí **http://localhost:5173**. La primera vez, `./mvnw` descarga Maven 3.9.9 y las dependencias.

Verificación rápida:

```bash
make test     # 23 tests backend + typecheck/build frontend
make smoke    # crea, lista, actualiza, borra y restaura el total vía API real
```

Salida esperada del smoke:

```text
OK   PostgREST responde
OK   Backend UP
OK   Total inicial: 4
OK   Tarea creada: 8f2c…
OK   La tarea aparece en el listado
OK   Estado actualizado a DONE
OK   Tarea eliminada (204)
OK   La tarea ya no está en el listado
OK   Total restaurado: 4
```

### Todo en Docker

```bash
make full-up     # construye backend y frontend, publica http://localhost:8081
make full-down
```

Nginx sirve la SPA y hace de proxy de `/api` hacia el backend; PostgREST y PostgreSQL no se publican.

## API

Base: `http://localhost:8080/api` · JSON camelCase · fechas ISO‑8601.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/tasks?status=TODO&page=0&size=20` | Listado paginado (`status` opcional, `size` ≤ 100) |
| `GET` | `/api/tasks/{id}` | Detalle o `404` |
| `POST` | `/api/tasks` | Crea (`201` + `Location`) |
| `PATCH` | `/api/tasks/{id}` | Actualiza solo los campos enviados |
| `DELETE` | `/api/tasks/{id}` | Borra (`204`) o `404` |

```bash
# Listar
curl -s 'http://localhost:8080/api/tasks?size=2' | python3 -m json.tool

# Crear
curl -s -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Preparar demo","priority":"HIGH","dueDate":"2030-12-31"}'

# Cambiar estado
curl -s -X PATCH http://localhost:8080/api/tasks/<id> \
  -H 'Content-Type: application/json' -d '{"status":"DONE"}'

# Borrar
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE http://localhost:8080/api/tasks/<id>
```

Forma de error uniforme (sin stacktraces):

```json
{
  "timestamp": "2026-10-18T21:47:40.332-03:00",
  "status": 400,
  "error": "Bad Request",
  "message": "title: El título es obligatorio",
  "path": "/api/tasks"
}
```

Detalle de campos, defaults y contrato: [`backend/src/main/java/com/example/taskflow/web`](backend/src/main/java/com/example/taskflow/web) y [`frontend/src/api/types.ts`](frontend/src/api/types.ts).

## Datos y PostgREST

- Esquema expuesto: `api` (nunca `public`).
- Tabla `api.tasks` con constraints, índice `(status, created_at desc)` y trigger `updated_at`.
- Roles de desarrollo: `authenticator` (login) y `web_anon` (anon). **Sin RLS todavía** (ver [Seguridad](#seguridad)).
- Los scripts de [`db/init/`](db/init) se ejecutan **solo al crear el volumen**.

```bash
# Consulta directa para depurar (solo desarrollo)
curl -s 'http://localhost:3000/tasks?select=id,title,status&status=eq.TODO&limit=3' | python3 -m json.tool

# Consola SQL
docker compose exec db psql -U taskflow -d taskflow -c 'select id, title, status from api.tasks;'

# Recrear la base desde cero (destructivo)
make db-reset
```

Sintaxis PostgREST, roles y RLS: skill `datos-postgrest`.

## Agentes de programación

Este repo está preparado para que **DSH** (DeepSeek Harness) trabaje con contexto y roles, no improvisando.

- [`AGENTS.md`](AGENTS.md): reglas del proyecto, fronteras, contrato, quality gates y deuda conocida.
- [`.dsh/skills/`](.dsh/skills): 6 skills que DSH descubre automáticamente al abrir la carpeta raíz.
- [`.dsh/agents/`](.dsh/agents): 5 briefs listos para `subagent`, con alcance y reporte obligatorio.

### Skills

| Skill | Usala cuando… |
|---|---|
| `arquitectura-fullstack` | agregás una entidad/recurso, cambiás el contrato o discutís una frontera |
| `backend-spring-boot` | tocás endpoints, validación, `RestClient`, errores o tests Java |
| `frontend-react-ts` | tocás componentes, hooks, formulario, estilos o el cliente API |
| `datos-postgrest` | cambiás esquema, roles, RLS, índices o consultas PostgREST |
| `verificacion-fullstack` | necesitás probar, diagnosticar por capa o preparar un PR |
| `revision-codigo` | querés un code review priorizado antes de cerrar |

En DSH podés invocarlas por nombre (por ejemplo: *“cargá `backend-spring-boot` y agregá PATCH de prioridad”*)
o dejar que el agente las elija por la tarea. También son archivos normales: editalas para adaptarlas a tu equipo.

> DSH detecta `.dsh/skills` porque el repo tiene `.git` en la raíz. Si copiás el proyecto sin git,
> abrí DSH en esta misma carpeta (el cwd actúa como raíz) o volvé a `git init`.
>
> Si ya tenías una sesión de DSH abierta **antes** de crear el repo, el catálogo de skills puede estar
> cacheado. Abrí una sesión nueva o ejecutá `touch .dsh/skills/*/SKILL.md` para forzar el refresco.

### Subagentes

Cada brief es autocontenido: se pasa como prompt y el subagente lee `AGENTS.md` + su skill.

| Brief | Área | Puede editar |
|---|---|---|
| [`data-dev.md`](.dsh/agents/data-dev.md) | esquema, roles, compose, Docker | `db/`, `compose*.yaml`, Dockerfiles |
| [`backend-dev.md`](.dsh/agents/backend-dev.md) | endpoints, validación, tests | `backend/` |
| [`frontend-dev.md`](.dsh/agents/frontend-dev.md) | UI, hooks, tipos, estilos | `frontend/` |
| [`reviewer.md`](.dsh/agents/reviewer.md) | revisión P0/P1/P2 | nada (solo reporta) |
| [`integrator.md`](.dsh/agents/integrator.md) | builds, smoke, diagnóstico | `scripts/`, fixes de integración |

Ejemplo de delegación (desde una sesión de DSH en esta carpeta):

```text
subagent(
  description: "Backend: endpoint de comentarios",
  prompt: "Leé .dsh/agents/backend-dev.md y .dsh/skills/backend-spring-boot/SKILL.md.
           Tarea: agregar comentarios por tarea (tabla api.comments ya existe).
           Contrato: GET/POST /api/tasks/{id}/comments, error shape actual.
           No toques frontend. Reportá tests y riesgos."
)
```

Flujo recomendado para una feature:

1. `arquitectura-fullstack` → fijar contrato.
2. `data-dev` → tabla/grants; `backend-dev` → endpoint y tests; `frontend-dev` → UI contra el contrato.
3. `integrator` → `make test` + `make smoke`.
4. `reviewer` → bloqueantes; el dueño aplica.

## Estructura

```text
.
├── AGENTS.md                 Reglas y quality gates
├── Makefile                  Atajos de desarrollo y verificación
├── compose.yaml              Postgres + PostgREST (dev)
├── compose.full.yaml         Stack completo (Nginx + backend + datos)
├── db/init/                  Roles, esquema, seed (numerados)
├── backend/                  Spring Boot 3.3 · Java 17
│   ├── mvnw / mvnw.cmd       Maven Wrapper
│   └── src/main/java/com/example/taskflow/
│       ├── config/           RestClient, CORS, propiedades
│       ├── domain/           Task, enums
│       ├── postgrest/        Cliente + mapeo snake_case
│       ├── service/          Reglas y normalización
│       ├── web/              Controller, DTOs, paginación
│       └── error/            ApiError + handler global
├── frontend/                 React 19 + TS + Vite
│   ├── src/api/              Tipos y cliente fetch
│   ├── src/hooks/            useTasks
│   └── src/components/       Form, lista, filtros, paginación
├── scripts/                  smoke.sh, dev.sh
├── docs/                     Capturas y decisiones (ADR)
└── .dsh/                     Skills y briefs de agentes
```

## Personalizar el dominio

Para pasar de “tareas” a tu entidad (facturas, tickets, pedidos):

1. **Datos**: nuevo `db/init/004_*.sql` con tabla, constraints, índice y grants. Probá con `curl` directo.
2. **Contrato**: definí JSON y códigos; actualizá `web/` (DTOs + controller) y `frontend/src/api/types.ts`.
3. **Backend**: repositorio PostgREST + servicio + tests.
4. **Frontend**: hook, lista y formulario; mantené estados de carga/vacío/error.
5. **Verificación**: `make test && make smoke`; agregá el recurso al README.
6. **Limpieza**: borrá el módulo `tasks` cuando ya no lo uses.

## Seguridad

> **Este starter es solo para desarrollo.** `web_anon` puede leer y escribir sin autenticación y no hay RLS.

Antes de exponer datos reales:

- Proveedor de identidad + JWT firmado; PostgREST valida el token (`PGRST_JWT_SECRET` real).
- Rol `app_user` y **RLS por dueño** (`owner_id` + `sub` del JWT), con tests de aislamiento.
- Cambiar credenciales de `authenticator` (hoy están en `compose.yaml` y `db/init/001_roles.sql`).
- PostgREST y PostgreSQL en red privada; solo el frontend/ALB público.
- HTTPS, rate limiting, backups y observabilidad.
- Revisar `docs/decisiones.md` (ADR-003) y el checklist de `revision-codigo`.

## Troubleshooting

| Problema | Causa / solución |
|---|---|
| `port is already allocated` | Liberá `5432`, `3000`, `8080` u `8081`, o cambiá el puerto en `.env` |
| PostgREST 401/403 | Roles/grants no aplicados o volumen viejo → `make db-reset` |
| Backend 502 | PostgREST no responde o `POSTGREST_URL` incorrecto → `docker compose logs postgrest` |
| `Invalid HTTP method: PATCH` | Regresión del cliente: usá `JdkClientHttpRequestFactory` (ya configurado) |
| CORS 403 en dev | Agregá tu origen a `CORS_ALLOWED_ORIGINS` (incluye `localhost` y `127.0.0.1`) |
| `npm install` con `EPERM` en `_cacache` | Entorno restringido/sandbox: `npm install --cache /tmp/npm-cache` |
| Cambios de esquema no aparecen | Los init scripts corren una vez → `make db-reset` o `NOTIFY pgrst, 'reload schema'` |
| Las skills no aparecen en DSH | Catálogo cacheado por sesión → sesión nueva o `touch .dsh/skills/*/SKILL.md` |
| `./mvnw` no descarga Maven | Sin red o proxy: instalá Maven 3.9 y usá `make backend MVN=mvn` |

## Roadmap

- Auth JWT + RLS y tests de aislamiento.
- Migraciones Flyway/Liquibase (hoy init de Docker).
- Playwright E2E + GitHub Actions (hoy verificación local con `make test`/`make smoke`).
- OpenAPI generado y cliente TS derivado.
- Observabilidad (trazas, métricas de negocio) y rate limiting.

---

Hecho para arrancar rápido y para que los agentes trabajen con las mismas reglas que el equipo.
