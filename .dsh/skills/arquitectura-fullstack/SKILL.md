---
name: arquitectura-fullstack
description: Diseñar o extender un recurso end-to-end en este starter Spring Boot + React TS + PostgREST. Fija fronteras, contrato y orden de implementación.
whenToUse: Cuando haya que agregar una entidad/recurso, cambiar el contrato /api, decidir si una consulta va por Spring o por PostgREST, o revisar coherencia entre capas.
---

# Arquitectura full-stack (TaskFlow)

Leé `AGENTS.md` antes de decidir. Esta skill evita el error clásico: escribir tres implementaciones
incoherentes del mismo recurso.

## La frontera, en una frase

**React habla solo con Spring Boot; Spring Boot habla con PostgREST; PostgREST habla con PostgreSQL.**
Si una propuesta rompe esa línea, necesita una decisión registrada en `docs/decisiones.md`.

## Qué va en cada capa

| Necesidad | Capa | Motivo |
|---|---|---|
| Validar forma/tipos, mensajes de error estables | Spring | Contrato único para el frontend |
| Reglas de negocio, transiciones de estado, permisos | Spring | Lógica imperativa y testeable |
| Agregar datos de varias tablas para una pantalla | Spring | Evita N llamadas desde el navegador |
| CRUD y filtros declarativos simples | PostgREST | Menos código, SQL directo |
| Paginación, orden, búsqueda por columna | PostgREST | `limit/offset/order`, `Prefer: count=exact` |
| Autorización por fila | Postgres (RLS) | No depende del cliente |
| Estado de UI, caché de pantalla, formularios | React | Nada de esto pertenece al backend |

Regla práctica: si la operación se puede expresar como **una** consulta HTTP a PostgREST, no escribas
un servicio Java que la reimplemente. Si necesita varias consultas, transacción o decisión, va en Spring.

## Flujo de una petición (alta de tarea)

```text
POST /api/tasks
  → TaskController  (@Valid CreateTaskRequest)
  → TaskService     (normaliza, reglas: dueDate no pasada, status inicial)
  → TaskRepository  (POST /tasks, Prefer: return=representation)
  → PostgREST       (INSERT ... RETURNING *)
  → 201 + Location: /api/tasks/{id}
```

El frontend recibe `Task` (camelCase). PostgREST devuelve snake_case: el mapeo vive **solo** en
`backend/.../postgrest/` (`@JsonProperty` o `PropertyNamingStrategies.SNAKE_CASE` por DTO).

## Checklist para agregar un recurso (en este orden)

1. **Contrato primero**: escribí el JSON de request/response y los códigos de error en el issue o en
   `docs/`. Sin esto no hay nada que implementar.
2. **Datos**: tabla en `db/init/00N_*.sql` (esquema `api`), constraints, índices para filtros reales,
   grants a `web_anon` (dev). Probá con `curl` directo a PostgREST.
3. **Backend**: DTOs (record), client PostgREST, service, controller, handler de errores, tests.
4. **Tipos del frontend**: `frontend/src/api/types.ts` espeja el contrato antes de tocar componentes.
5. **Cliente y hook**: `api/client.ts` + hook con estados `loading | error | data`; AbortController.
6. **UI**: lista, formulario, estados vacío/error/carga, accesibilidad.
7. **Verificación**: `make test` y `make smoke` (skill `verificacion-fullstack`).
8. **Documentación**: endpoint nuevo en README y decisión en `docs/decisiones.md` si cambió una frontera.

## Decisiones ya tomadas (no re-litigar sin motivo)

- **BFF, no BFF innecesario**: Spring no reimplementa cada filtro; expone lo que el cliente necesita.
- **Sin ORM**: PostgREST + JDBC no aplican; el acceso pasa por HTTP. No agregues JPA/Hibernate.
- **Sin auth en el starter**: hay un único rol anónimo de desarrollo. Producción exige JWT + RLS antes
  de exponer datos reales; está en el roadmap.
- **Sin GraphQL ni tRPC**: el contrato es REST/JSON.
- **CSS plano**: la UI es deliberadamente simple y accesible; no introduzcas Tailwind/MUI sin ADR.

## Señales de diseño equivocado

- El frontend arma URLs con `?select=` o conoce el nombre del esquema → fuga de la frontera.
- El backend devuelve la fila cruda de PostgREST con `snake_case` → el contrato se filtró.
- Varios endpoints que solo cambian un `WHERE` → un endpoint con filtros validados.
- Lógica de permisos en React → se salta con DevTools.
- Un `TaskService` con 40 métodos para operaciones que PostgREST ya resuelve → sobre-ingeniería.

## Cómo extender el starter a otro dominio

Renombrá el dominio (`Task` → `Invoice`, etc.) manteniendo el patrón:

```bash
# 1) tabla + grants + seed   → db/init/004_invoices.sql
# 2) espejo Java             → backend/src/main/java/com/example/taskflow/{domain,postgrest,service,web}
# 3) tipos TS                → frontend/src/api/types.ts
# 4) hook + componentes      → frontend/src/hooks, frontend/src/components
```

No copies y pegues el módulo `tasks` completo sin borrar lo que no aplique. El starter debe quedar
ejecutable en todo momento (`make test`, `make smoke`).

## Definition of done (arquitectura)

- [ ] El navegador no conoce PostgREST ni sus parámetros.
- [ ] El contrato está documentado y tipado en ambos lados.
- [ ] Errores 400/404/409/502 tienen el mismo shape.
- [ ] Migración de datos versionada y reversible en desarrollo (`make db-reset`).
- [ ] `make test` + `make smoke` verdes, con evidencia pegada en el cierre.
