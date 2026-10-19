---
name: datos-postgrest
description: Modelar PostgreSQL y exponerlo con PostgREST de forma segura. Esquema, roles, RLS, sintaxis de consultas y migraciones.
whenToUse: Al crear tablas/índices/roles, escribir consultas PostgREST, depurar 401/403/PGRST116 o preparar RLS y auth.
---

# Datos: PostgreSQL + PostgREST

## Modelo del starter

Esquema `api`, tabla `api.tasks`. Todo cambio arranca en `db/init/` (numerado) y se prueba contra el
contenedor real, no contra una idea.

```sql
create table if not exists api.tasks (
  id          uuid primary key default gen_random_uuid(),
  title       text not null check (length(btrim(title)) between 1 and 200),
  description text check (length(description) <= 2000),
  status      text not null default 'TODO' check (status in ('TODO','IN_PROGRESS','DONE')),
  priority    text not null default 'MEDIUM' check (priority in ('LOW','MEDIUM','HIGH')),
  due_date    date,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
```

Convenciones:
- snake_case en DB; el mapeo a camelCase ocurre en el BFF, no en SQL con alias raros.
- `uuid` + `gen_random_uuid()`; no secuencias visibles.
- `timestamptz` siempre; `date` para fechas sin hora.
- Constraints en la base además de validación en Java (defensa en profundidad).
- Índices solo para filtros/orden reales: `(status, created_at desc)`.

## Roles mínimos

```sql
create role authenticator noinherit login password '...';
create role web_anon nologin;
grant web_anon to authenticator;              -- permite SET ROLE
grant usage on schema api to web_anon;
grant select, insert, update, delete on api.tasks to web_anon;
```

- PostgREST conecta como `authenticator` y hace `SET ROLE web_anon` (`PGRST_DB_ANON_ROLE`).
- `web_anon` es **solo desarrollo**. Producción: `app_user` + JWT (`role` claim) y RLS.
- Nunca des `superuser` a `authenticator`; nunca expongas `public`.

## RLS (cuando llegue auth)

```sql
alter table api.tasks enable row level security;
create policy tasks_owner on api.tasks
  using (owner_id = (current_setting('request.jwt.claims', true)::jsonb ->> 'sub')::uuid)
  with check (owner_id = (current_setting('request.jwt.claims', true)::jsonb ->> 'sub')::uuid);
```

- El rol de la API **no** debe ser dueño de la tabla (el owner salta RLS salvo `FORCE`).
- Probá con `set role app_user; set request.jwt.claims = '{"sub":"..."}';`.
- PostgREST cachea el esquema: tras DDL, `NOTIFY pgrst, 'reload schema';` (o `PGRST_DB_SCHEMA` fresh).

## Sintaxis PostgREST que vas a usar

| Necesidad | Query |
|---|---|
| Filtrar igualdad | `/tasks?status=eq.TODO` |
| Múltiples valores | `/tasks?status=in.(TODO,IN_PROGRESS)` |
| Texto | `/tasks?title=ilike.*deploy*` |
| Nulos | `/tasks?due_date=is.null` |
| Orden + paginación | `/tasks?order=created_at.desc&limit=20&offset=0` |
| Total exacto | Header `Prefer: count=exact` → `Content-Range: 0-19/42` |
| Insertar y devolver | `POST /tasks` + `Prefer: return=representation` |
| Actualizar | `PATCH /tasks?id=eq.{uuid}` + `return=representation` |
| Borrar | `DELETE /tasks?id=eq.{uuid}` → 204 |
| Upsert | `POST /tasks?on_conflict=id` + `Prefer: resolution=merge-duplicates` |
| Columnas | `select=id,title,status` |

Codificá espacios/operadores en el BFF con `UriBuilder`; no concatenes strings crudos del usuario.

## Migraciones y seed

- `db/init/001_roles.sql`, `002_schema.sql`, `003_seed.sql` corren **solo** al crear el volumen.
- Un cambio nuevo → `004_descripcion.sql`; nunca edites uno ya desplegado.
- `make db-reset` destruye el volumen y re-siembra: usalo en desarrollo, jamás en producción.
- Seed idempotente (`on conflict do nothing`) para que el reset no falle.
- En producción: Flyway/Liquibase o pipeline de migraciones; el init de Docker no es suficiente.

## Depuración

```bash
docker compose logs -f postgrest           # errores de conexión/rol
curl -i 'http://localhost:3000/tasks?limit=1'
docker compose exec db psql -U taskflow -d taskflow -c '\d+ api.tasks'
docker compose exec db psql -U taskflow -d taskflow -c 'select * from api.tasks;'
```

| Síntoma | Causa probable |
|---|---|
| 401 `role "web_anon" does not exist` | init no corrió / volumen viejo |
| 403 `permission denied for table` | falta GRANT o RLS sin policy |
| `PGRST116` | `.single()` implicito: el Accept pide objeto y hay 0/muchas filas |
| 404 en `/tasks` | tabla fuera del esquema expuesto (`PGRST_DB_SCHEMAS`) |
| columnas nuevas no aparecen | caché de esquema: `NOTIFY pgrst, 'reload schema'` |

## Checklist

- [ ] DDL nuevo numerado, idempotente y probado desde volumen limpio.
- [ ] Grants mínimos; sin `web_anon` en producción.
- [ ] Índices para los filtros del endpoint real.
- [ ] `curl` directo documentado para cada endpoint nuevo.
