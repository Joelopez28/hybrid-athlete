---
name: arquitectura-fullstack
description: Diseñar y extender RUKY (entrenamiento CrossFit, powerlifting, calistenia y HYROX) sin romper la frontera React → Spring Boot → PostgREST → PostgreSQL.
whenToUse: Al agregar una funcionalidad de producto, cambiar el contrato /api, modelar una disciplina o decidir dónde vive un cálculo.
---

# Arquitectura de RUKY

Leé `AGENTS.md` primero. Esta skill decide **dónde va cada cosa** antes de escribir código.

## La frontera

**React solo habla con Spring Boot; Spring Boot es el único cliente de PostgREST; PostgreSQL es la
fuente de verdad.**

```text
Registrar sesión:
React form → POST /api/sessions
  → SessionController (@Valid)
  → SessionService (normaliza, valida reglas)
  → SessionRepository → POST /rest/rpc/create_session  (transacción: sesión + series)
  → findById → GET /training_sessions?select=*,session_entries(*)
  → 201 SessionResponse (camelCase, volumeKg calculado)
```

## Qué va en cada capa

| Necesidad | Capa | Motivo |
|---|---|---|
| Validar campos, mensajes de error estables | Spring | Contrato único |
| Calcular volumen, PRs, ranking, rachas | Spring | Lógica y tests |
| Insertar sesión + N series sin estados a medias | PostgreSQL (RPC) | Atomicidad |
| CRUD y lecturas con filtros/embedding | PostgREST | Menos código |
| Autorización por fila (futuro) | PostgreSQL RLS | No depende del cliente |
| Estado de pestañas, formularios, filtros de UI | React | No pertenece al backend |

Regla: si es **una** consulta declarativa, no la reimplementes en Java. Si hay varias tablas,
transacción, agregación o decisión de negocio, va en Spring o en una función SQL.

## Modelo mental del dominio

- `athlete`: identidad, división (`OPEN`/`ELITE`), peso, disciplinas.
- `exercise`: catálogo compartido; cada uno tiene disciplina, categoría y unidad.
- `training_session`: fecha, disciplina, tipo, duración, RPE, resultado de WOD/HYROX.
- `session_entry`: una serie o hold; `reps`, `weight_kg`, `seconds`.
- **Volumen** = Σ reps × kg (solo peso externo). **PR** = mejor marca de sesiones `STRENGTH`/`SKILL`
  por ejercicio; holds usan segundos.

Las cuatro disciplinas comparten el mismo modelo; no crees una tabla por disciplina. Lo específico
(WOD, HYROX, skills) se expresa con `session_type` + campos de resultado + entradas.

## Categorías (no confundir con el modelo de entrenamiento)

| Disciplina | Divisiones reales (referencia) |
|---|---|
| CrossFit | RX / Intermediate / Scaled; Masters; equipos; adaptado |
| Powerlifting | sexo, peso corporal, Junior/Open/Master, Raw/Equipped |
| HYROX | Open / Pro / Doubles / Relay; age groups |
| Calistenia | streetlifting (peso), freestyle, endurance, skills |

Hoy `athletes.tier` solo distingue `OPEN`/`ELITE` para el leaderboard. Las categorías finas y las
competencias son fase 2: no inventes una taxonomía universal de kilos.

## Checklist para agregar una funcionalidad

1. **Producto**: escribí el caso de uso y el contrato JSON (request, response, errores).
2. **Datos**: ¿alcanza el esquema? Si no, `db/init/006_*.sql` + grants; probá con `curl`.
3. **Backend**: DTO + validación + repository/service + tests.
4. **Tipos TS**: espejo exacto en `frontend/src/api/types.ts` antes de tocar componentes.
5. **UI**: página/componente + estados carga/vacío/error + accesibilidad.
6. **Verificación**: `make test`, `make smoke` y recorrido E2E.
7. **Docs**: README/AGENTS si cambia contrato o regla.

## Competencias (implementadas)

- Tablas: `competitions`, `competition_divisions`, `competition_events`,
  `competition_registrations`, `competition_results`.
- Flujo: `Compete` → `CompetitionController` → `CompetitionService` → `CompetitionRepository`.
- El puntaje vive en `CompetitionScoring`, **no** en `LeaderboardService` ni en React.
- Inscripción y resultado usan upsert (`on_conflict` + `resolution=merge-duplicates`).
- Una competencia debe estar `LIVE` para inscribirse/cargar resultados; Ruky Elite exige tier ELITE.

## Siguiente fase

- **Admin de competencias**: validar resultados, cerrar eventos y publicar ranking final.
- **Ruky Elite**: criterios oficiales + postulación; no uses el ranking de volumen como clasificación.
- **Auth/RLS**: reemplazar `CurrentAthleteService` por el principal autenticado; el resto del dominio
  no debería cambiar.

## Señales de diseño equivocado

- El frontend arma `select=...` o conoce PostgREST → fuga de frontera.
- Una tabla `wods`, otra `hyrox_sessions`, otra `lifts` → duplicación del mismo concepto.
- El backend devuelve filas crudas en snake_case → contrato filtrado.
- Calcular PRs o rankings en React → no se puede auditar ni comparar entre atletas.
- Simular auth con un header “X-User” cuando no hay verificación real.

## Definition of done

- [ ] Contrato documentado y tipado en ambos lados.
- [ ] Cálculos (volumen/PR/ranking) viven en backend y tienen test.
- [ ] Errores 400/404/409/502 con el mismo shape.
- [ ] `make test` + `make smoke` verdes; UI probada en desktop y móvil.
