# Decisiones de arquitectura (ADR)

Registro corto de decisiones que no queremos re-litigar en cada sesión. Formato: fecha, contexto,
decisión, consecuencias. Si cambiás una, agregá un ADR nuevo que la reemplace.

## ADR-001 — Spring Boot como BFF delante de PostgREST

- **Fecha**: 2026-10-18
- **Contexto**: necesitamos un contrato estable y validación, pero también aprovechar PostgREST para
  CRUD declarativo sin escribir un ORM.
- **Decisión**: React llama solo a `/api` (Spring Boot). Spring valida, normaliza errores y orquesta;
  PostgREST queda en la red privada y es el único acceso a PostgreSQL.
- **Consecuencias**: una capa extra de latencia y despliegue; a cambio, el frontend no conoce el
  esquema, se pueden agregar reglas y auth sin tocar la UI, y PostgREST se reemplaza sin romper el
  contrato. El navegador nunca recibe credenciales de base de datos.

## ADR-002 — Sin JPA/Hibernate

- **Fecha**: 2026-10-18
- **Contexto**: el acceso a datos ya lo resuelve PostgREST vía HTTP; mapear las mismas tablas con JPA
  duplicaría el modelo y las migraciones.
- **Decisión**: `RestClient` + DTOs de transporte; SQL vive en `db/init` y en las consultas PostgREST.
- **Consecuencias**: no hay sesiones ni lazy loading; las transacciones complejas multi-tabla
  requerirán funciones RPC de PostgreSQL o un servicio dedicado, no un ORM agregado a mitad de camino.

## ADR-003 — Auth fuera del starter (explícito)

- **Fecha**: 2026-10-18
- **Contexto**: el objetivo es un esqueleto ejecutable, no un producto multi-tenant.
- **Decisión**: `web_anon` con permisos de desarrollo y sin RLS. La deuda está documentada y
  bloquea cualquier despliegue con datos reales.
- **Consecuencias**: cualquiera con acceso a la red puede leer/escribir. Antes de exponer: proveedor
  de identidad, JWT verificado por PostgREST, rol `app_user`, RLS por `owner_id`, y tests de
  aislamiento entre usuarios.

## ADR-004 — Migraciones con scripts de init (por ahora)

- **Fecha**: 2026-10-18
- **Contexto**: Docker ejecuta `db/init/*.sql` solo al crear el volumen, suficiente para desarrollo.
- **Decisión**: archivos numerados e idempotentes; sin Flyway todavía.
- **Consecuencias**: en producción hace falta una herramienta de migraciones (Flyway/Liquibase) o un
  pipeline que aplique los scripts con control de versión. `make db-reset` es la vía de desarrollo.

## ADR-005 — Estado remoto sin librería de data fetching

- **Fecha**: 2026-10-18
- **Contexto**: una sola pantalla y un solo recurso; agregar TanStack Query ahora sería infraestructura
  sin uso.
- **Decisión**: hook `useTasks` con `fetch` tipado y `AbortController`.
- **Consecuencias**: si aparecen caché entre pantallas, mutaciones optimistas complejas o router,
  migrar a TanStack Query con un ADR que documente la migración.
