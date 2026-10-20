---
name: revision-codigo
description: Revisar cambios de RUKY con checklist priorizado (datos, contrato, seguridad, cálculos, UI y tests) y salida accionable.
whenToUse: Antes de mergear o cerrar una tarea; al pedir una segunda opinión sobre un diff; al auditar privacidad o consistencia del ranking.
---

# Revisión de código (RUKY)

Revisá el diff completo, incluidos tests, SQL y seed. No reescribas: reportá. Si el autor pide
arreglos, primero entregá el informe.

## Orden de lectura

1. `git diff --stat` / `git diff`.
2. Contrato: `web/` DTOs ↔ `frontend/src/api/types.ts` ↔ README.
3. Datos: `db/init/*.sql` ↔ repositorios ↔ cálculos.
4. Tests: ¿fallan si se rompe el comportamiento?
5. Seguridad y accesibilidad.

## P0 — bloqueante

- [ ] Secreto/credencial en el diff (password real, JWT, URL privada).
- [ ] `athleteId` aceptado desde el request: hoy **todo** debe salir de `CurrentAthleteService`.
- [ ] Inyección en filtros/orden de PostgREST o SQL concatenado en la RPC.
- [ ] PR/volumen calculado en React o distinto entre endpoints.
- [ ] WOD tratado como PR (debe limitarse a `STRENGTH`/`SKILL`).
- [ ] Migración destructiva sin plan (`drop`, `alter type`) o edición de un init ya aplicado.
- [ ] Auth simulada (header “X-User”, token hardcodeado) presentada como real.
- [ ] Datos de otro atleta visibles por un id adivinable.
- [ ] Error 500 con stacktrace o body crudo de PostgREST.

## P1 — corregir antes de cerrar

- [ ] Contrato desincronizado entre Java y TS (campos nullable, enums, `bestSeconds`).
- [ ] RPC sin `{"payload": ...}` o sin reload de schema.
- [ ] Volumen = reps × kg mal aplicado (bodyweight, holds, series nulas).
- [ ] `setIndex` o orden de series inconsistente en la respuesta.
- [ ] Falta test de: creación con series, WOD inválido, ownership, PR por segundos, ranking Elite.
- [ ] `StatsService`/`LeaderboardService` hacen N+1 o traen más filas de las necesarias.
- [ ] Formulario sin validación de cliente o sin mostrar el error del servidor.
- [ ] Mobile con overflow horizontal (tabla de series, nav, cards).
- [ ] `any`, non-null assertions o casts que ocultan un error de tipos.

## P2 — mejora

- [ ] Nombres vagos (`data`, `handleClick`, `Manager`).
- [ ] Código muerto, imports sin usar, comentarios que repiten el código.
- [ ] CSS duplicado o valores fuera de tokens.
- [ ] Falta índice para el filtro nuevo.
- [ ] README/ADR desactualizado.
- [ ] Textos de UI en inglés o inconsistentes.

## Preguntas guía

- ¿Qué pasa si PostgREST devuelve 500 o tarda 8s? ¿Timeout y mensaje?
- ¿Dos sesiones simultáneas del mismo atleta corrompen algo? ¿La RPC es atómica?
- ¿El test falla si cambio `weight_kg` por `reps` en el volumen?
- ¿La UI funciona sin mouse y sin color?
- ¿El ranking sigue siendo comparable si alguien registra 10 sesiones de 5 minutos?
- ¿El cambio se puede revertir sin tocar datos?

## Formato de salida

```markdown
## Resumen
<2–3 líneas + veredicto>

## Bloqueantes
1. `archivo:línea` — impacto → fix sugerido.

## Importantes
- ...

## Menores
- ...

## Verificación
- Comandos y resultado; qué no se probó.

## Veredicto
APROBADO / APROBADO CON CAMBIOS / RECHAZADO
```

Reglas:
- Citá `archivo:línea` y explicá el impacto, no solo el estilo.
- Un hallazgo por ítem; sin duplicados.
- Si no hay bloqueantes, decilo y no inventes problemas.
- No propongas refactors grandes dentro de un fix chico; anotalos como seguimiento.
