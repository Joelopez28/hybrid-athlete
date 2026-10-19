# TaskFlow — instrucciones del workspace

Starter full-stack para construir productos sobre **Spring Boot (BFF) + React + TypeScript + PostgREST/PostgreSQL**.
Todo el equipo humano y los agentes de DSH siguen este archivo. Es la fuente de verdad del repositorio.

## Mapa del repo

```text
backend/        Spring Boot 3.3 · Java 17 · BFF y reglas de negocio
frontend/       React 19 + TypeScript + Vite · UI
db/init/        Esquema, roles y seed de PostgreSQL (se ejecuta al crear el volumen)
compose.yaml    Postgres + PostgREST para desarrollo
compose.full.yaml  Stack completo en contenedores (backend + frontend)
scripts/        Atajos de verificación y smoke tests
.dsh/skills/    Skills reutilizables del equipo de agentes
.dsh/agents/    Briefs de subagentes (backend, frontend, datos, reviewer)
docs/           Decisiones y notas de arquitectura
```

## Arquitectura (no improvisar esta frontera)

```text
React (5173) --/api--> Spring Boot (8080) --HTTP--> PostgREST (3000) --> PostgreSQL (5432)
```

- El **navegador nunca llama a PostgREST**. PostgREST queda en la red privada; Spring Boot es el único cliente.
- **Postgres es la fuente de verdad**. El esquema se versiona en `db/init/` (luego migraciones formales).
- **Spring Boot** aporta validación, contrato estable `/api`, manejo de errores, CORS, auth futura y orquestación.
- **PostgREST** resuelve lectura/escritura declarativa; no dupliques CRUD trivial en Java si el filtro se expresa bien en la URL.
- **React** solo conoce los tipos de `frontend/src/api/types.ts`; nunca construye URLs de PostgREST.

## Comandos

```bash
make up            # Postgres + PostgREST (docker compose)
make backend       # Spring Boot en :8080 (requiere Java 17)
make frontend      # Vite en :5173
make test          # tests backend + typecheck/build frontend
make smoke         # prueba real de la cadena API -> PostgREST -> DB
make down          # apagar contenedores
make db-reset      # borrar volumen y re-sembrar (destructivo)
```

Detalle y prerrequisitos en `README.md`.

## Contrato de la API

- Base: `/api/tasks`. JSON en camelCase; fechas ISO-8601 (`2026-10-18T21:00:00Z`).
- Listado paginado: `GET /api/tasks?status=TODO&page=0&size=20` →
  `{ items, page, size, total, totalPages }`.
- Errores uniformes: `{ timestamp, status, error, message, path }` (ver `backend/.../error/ApiError.java`).
- Códigos: 400 validación, 404 no existe, 409 conflicto, 502 PostgREST caído. Nunca devolver stacktraces.
- Cambiar el contrato exige: actualizar backend, `frontend/src/api/types.ts`, tests y README.

## Reglas de datos

- IDs `uuid` generados por la base (`gen_random_uuid()`), nunca por el cliente.
- `created_at` / `updated_at` con `timestamptz`; `updated_at` lo mantiene un trigger.
- Toda tabla vive en el esquema `api`, no en `public`.
- Roles: `authenticator` (login, `NOINHERIT`), `web_anon` (solo dev). En producción añadir `app_user` + RLS por `sub` del JWT.
- Los cambios de esquema van en un archivo nuevo y numerado (`db/init/004_*.sql`); no edites uno ya aplicado en producción.

## Calidad obligatoria antes de dar algo por terminado

1. `cd backend && mvn test` (o `make test`) verde.
2. `cd frontend && npm run typecheck && npm run build` verde.
3. `make smoke` verde si tocaste API, PostgREST o esquema.
4. Sin secretos reales en el repo; usar `.env` (ignorado) y `.env.example`.
5. Accesibilidad: labels asociados, foco visible, contraste AA, navegación por teclado.

## Convenciones de código

- **Inglés** para identificadores, nombres de archivos, commits y comentarios técnicos.
- **Español** para textos de UI, README y documentación de producto.
- Java: records para DTOs, `Optional` para búsquedas, constructor injection, sin Lombok.
- TypeScript: `strict`, sin `any`, un tipo por contrato, estado derivado en vez de duplicado.
- CSS plano con variables; sin frameworks de UI salvo decisión registrada en `docs/`.
- Commits: `feat|fix|refactor|docs|test|chore(scope): mensaje` en imperativo.

## Cómo trabajamos con agentes (DSH)

1. **Arranque de sesión**: este `AGENTS.md` ya está cargado; no lo repitas.
2. **Elegí skill** según la tarea y cargala antes de editar:
   - arquitectura / recurso nuevo → `arquitectura-fullstack`
   - endpoints, validación, errores → `backend-spring-boot`
   - UI, hooks, cliente API → `frontend-react-ts`
   - esquema, roles, consultas PostgREST → `datos-postgrest`
   - probar / depurar la cadena completa → `verificacion-fullstack`
   - revisar un cambio antes de cerrar → `revision-codigo`
3. **Delegación**: los briefs de `.dsh/agents/` se pasan a subagentes con `subagent`. Un dueño por directorio:
   - backend → `backend/`
   - frontend → `frontend/`
   - datos/infra → `db/`, `compose*.yaml`
   - reviewer → no edita, reporta
4. **No tocar archivos de otra área** sin avisar; si el contrato cambia, actualizá `frontend/src/api/types.ts` y avisá al dueño.
5. **Cerrar con evidencia**: comandos ejecutados + resultado, archivos tocados y riesgos pendientes.

## Skills de DSH

| Skill | Cuándo |
|---|---|
| `arquitectura-fullstack` | diseñar o agregar un recurso end-to-end |
| `backend-spring-boot` | Java, endpoints, RestClient, tests |
| `frontend-react-ts` | React, TypeScript, forms, estado, CSS |
| `datos-postgrest` | SQL, roles, RLS, sintaxis PostgREST |
| `verificacion-fullstack` | build, smoke tests, debugging de la cadena |
| `revision-codigo` | code review con checklist y prioridades |

## Deuda técnica conocida (roadmap)

- Auth real (JWT de proveedor + RLS) y roles finos.
- Migraciones Flyway/Liquibase (hoy init scripts de Docker).
- Observabilidad (métricas de negocio, trazas) y rate limiting.
- E2E con Playwright y CI (GitHub Actions).
- Contratos OpenAPI generados y cliente TS derivado.

Registrá decisiones nuevas en `docs/decisiones.md` con fecha, contexto, decisión y consecuencias.
