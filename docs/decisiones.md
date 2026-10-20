# Decisiones de arquitectura (ADR)

Registro corto de decisiones que no queremos re-litigar en cada sesión. Formato: contexto, decisión,
consecuencias. Si cambiás una, agregá un ADR nuevo que la reemplace.

## ADR-001 — Spring Boot como BFF delante de PostgREST

- **Fecha**: 2026-10-20
- **Contexto**: necesitamos contrato estable, validación y cálculos (volumen, PRs, ranking), pero
  también aprovechar PostgREST para CRUD declarativo.
- **Decisión**: React llama solo a `/api`. Spring valida, agrega y orquesta; PostgREST queda privado.
- **Consecuencias**: una capa extra de latencia y despliegue; a cambio, el frontend no conoce el
  esquema y se pueden cambiar tablas o auth sin tocar la UI.

## ADR-002 — Un modelo único de sesiones para las cuatro disciplinas

- **Fecha**: 2026-10-20
- **Contexto**: CrossFit, powerlifting, calistenia y HYROX tienen métricas distintas; hacer una tabla
  por disciplina duplicaría pantallas y reportes.
- **Decisión**: `training_sessions` + `session_entries` con `discipline`, `session_type`, `reps`,
  `weight_kg` y `seconds`. Lo específico se expresa con campos de resultado (`wod_*`).
- **Consecuencias**: consultas y stats comparables entre disciplinas; los casos muy específicos
  (splits HYROX, intentos de powerlifting) requerirán columnas/tablas adicionales, no un rediseño.

## ADR-003 — Crear sesión con una RPC atómica

- **Fecha**: 2026-10-20
- **Contexto**: una sesión tiene N series; si el insert de series falla, no queremos una sesión vacía.
  El nested insert de PostgREST 12 no funcionó en este deployment.
- **Decisión**: `api.create_session(payload jsonb)` inserta sesión + series en una transacción y
  devuelve el id; el BFF luego lee la sesión con embeddings.
- **Consecuencias**: dos round-trips BFF→PostgREST y lógica en PL/pgSQL. Si se cambia la firma hay que
  actualizar la función, `SessionWrite` y los tests.

## ADR-004 — Modo demo sin auth (explícito)

- **Fecha**: 2026-10-20
- **Contexto**: validar producto y UI rápido, sin un proveedor de identidad.
- **Decisión**: todas las escrituras pertenecen al atleta `DEMO_ATHLETE_HANDLE` (default `ruky`);
  `web_anon` tiene permisos de desarrollo y no hay RLS.
- **Consecuencias**: no es multi-usuario ni seguro para datos reales. **Bloqueante** para producción:
  auth + `app_user` + RLS + tests de aislamiento.

## ADR-005 — PRs solo de fuerza/skills

- **Fecha**: 2026-10-20
- **Contexto**: los WOD guardan reps agregadas (ej. 45 thrusters); usarlas como PR produce marcas
  engañosas.
- **Decisión**: los PRs se calculan solo sobre sesiones `STRENGTH` y `SKILL`. Comparan peso, luego
  reps, luego segundos (holds). El volumen sí incluye entradas de WOD.
- **Consecuencias**: un WOD con carga pesada no genera PR automáticamente; si se quiere, se registra
  como sesión de fuerza. La UI lo explica en “Mejor marca por ejercicio”.

## ADR-006 — Ruky Elite es una división, no una suscripción

- **Fecha**: 2026-10-20
- **Contexto**: el usuario quiere una “categoría Ruky Elite” dentro de la comunidad.
- **Decisión**: `athletes.tier ∈ {OPEN, ELITE}` y el leaderboard filtra por división. Los estándares
  mostrados son de referencia; el acceso es por invitación en esta fase.
- **Consecuencias**: no hay cobro ni gating de contenido. La clasificación automática por benchmarks
  es una fase posterior y requiere definir reglas oficiales.

## ADR-007 — Competencias Strong Fit fuera del MVP

- **Fecha**: 2026-10-20
- **Contexto**: el producto final incluye competencias con varias pruebas y ranking por puntos, pero
  el MVP priorizó registro + progreso.
- **Decisión**: no implementar el motor todavía; dejar el modelo de sesiones y el leaderboard como
  base. La UI muestra el plan en Comunidad.
- **Consecuencias**: cuando se implemente, crear tablas `competitions`, `competition_events`,
  `registrations`, `event_results` y un servicio de scoring separado de `LeaderboardService`.
