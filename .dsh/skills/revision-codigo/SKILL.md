---
name: revision-codigo
description: Revisar cambios de este stack con checklist priorizado (correctitud, contrato, seguridad, datos, tests, accesibilidad) y salida accionable.
whenToUse: Antes de mergear o cerrar una tarea; al pedir una segunda opinión sobre un diff; al auditar seguridad o consistencia del contrato.
---

# Revisión de código

Revisá el **diff completo** (incluidos tests y migraciones), no solo los archivos que te nombran.
No reescribas el cambio: reportá. Si el autor te pide arreglar, primero entregá el informe.

## Orden de lectura

1. `git diff --stat` y `git diff` (o el rango de commits).
2. Contrato: `web/` DTOs ↔ `frontend/src/api/types.ts` ↔ README.
3. Persistencia: `db/init/*.sql` ↔ `postgrest/` ↔ `service/`.
4. Tests: ¿prueban comportamiento o implementación? ¿Falta el caso de error?
5. Seguridad y accesibilidad.

## Checklist priorizado

### P0 — bloqueante (no mergear)

- [ ] Secreto/credencial en el diff (password, token, URL privada con auth).
- [ ] Inyección: SQL concatenado, `select`/`order`/`limit` sin validar, URL de PostgREST con input crudo.
- [ ] IDOR: un endpoint acepta `id` del cliente sin verificar pertenencia/permiso.
- [ ] Validación ausente en escritura (título vacío, estado inválido, longitud).
- [ ] `any`/`!` que oculta un error de tipos en el contrato.
- [ ] Migración destructiva (`drop`, `alter type`) sin plan de rollback.
- [ ] Error 500 con stacktrace o detalles internos expuestos.
- [ ] El frontend llama a PostgREST o conoce el esquema.

### P1 — debería corregirse antes de cerrar

- [ ] Contrato desincronizado (campo nuevo en Java, no en TS; o viceversa).
- [ ] Error shape distinto entre endpoints.
- [ ] Falta test del caso de error principal (400/404/409).
- [ ] `PATCH` pisa columnas con `null` no intencionales.
- [ ] Paginación sin `total` o `total` mal calculado (`Content-Range`).
- [ ] Race condition en el hook (respuesta vieja pisa la nueva; falta AbortController).
- [ ] Formulario sin label/`aria-invalid`; foco no visible.
- [ ] N+1 de requests desde React (una llamada por fila).
- [ ] Log de nivel `info` en cada request; ruido en producción.

### P2 — mejora

- [ ] Nombres vagos (`data`, `handleClick2`, `Manager`).
- [ ] Código muerto, imports sin usar, comentarios que repiten el código.
- [ ] CSS duplicado; valores mágicos fuera de tokens.
- [ ] Falta índice para el filtro nuevo.
- [ ] README/ADR desactualizado.

## Preguntas guía

- ¿Qué pasa si PostgREST devuelve 500 o tarda 30s? ¿Hay timeout y mensaje?
- ¿Qué pasa con dos usuarios simultáneos? ¿Se pierde una actualización?
- ¿El test falla si rompo la query de PostgREST? Si no, no prueba nada.
- ¿La UI funciona sin mouse y sin color?
- ¿El cambio se puede revertir sin tocar datos?

## Formato de salida

```markdown
## Resumen
<2–3 líneas: qué hace el cambio y veredicto>

## Bloqueantes
1. `archivo:línea` — problema concreto → fix sugerido.

## No bloqueantes
- ...

## Verificación
- Comandos que corriste y resultado; qué faltó probar.

## Veredicto
APROBADO / APROBADO CON CAMBIOS / RECHAZADO
```

Reglas:
- Citá `archivo:línea` y explicá el **impacto**, no solo el estilo.
- Un hallazgo por ítem; sin duplicados.
- Si no hay bloqueantes, decilo explícitamente y no inventes problemas.
- No propongas refactors grandes dentro de un fix pequeño; anotalos como seguimiento.
