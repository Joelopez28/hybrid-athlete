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

## ADR-004 — Auth JWT + RLS para aislar atletas

- **Fecha**: 2026-10-20
- **Contexto**: el modo demo con un único atleta no sirve para usuarios reales; las sesiones y
  credenciales deben quedar aisladas.
- **Decisión**: registro/login en el BFF con BCrypt; el backend emite un JWT HS256 (`sub` = athlete
  UUID, `role` = `app_user`) firmado con el mismo secreto que PostgREST. PostgREST valida el token y
  ejecuta como `app_user`; RLS limita sesiones y series al dueño. `athlete_credentials` no tiene
  grants para `app_user`.
- **Consecuencias**: se requiere `PGRST_JWT_SECRET` compartido y HTTPS. El JWT dura 12 h y no hay
  refresh/revocación. El frontend guarda el token en `localStorage` (riesgo XSS aceptado en MVP).

## ADR-009 — Rol de servicio y vista agregada para la comunidad

- **Fecha**: 2026-10-20
- **Contexto**: el backend necesita leer credenciales por email/handle y registrar atletas antes de
  que exista un usuario autenticado; además el leaderboard comunitario necesita agregados de todos
  los atletas sin exponer sus sesiones.
- **Decisión**: `app_service` (BYPASSRLS) se usa **solo** en el cliente interno del BFF para auth.
  Las requests de usuario usan `app_user`. El leaderboard lee
  `api.athlete_weekly_activity`, una vista propiedad del rol de migración que agrega volumen,
  sesiones y minutos por atleta/semana.
- **Consecuencias**: el token de servicio nunca sale del backend. Cualquier cambio en la vista o en
  las políticas RLS debe cubrirse con tests de aislamiento. Los resultados de competencia y los
  perfiles siguen siendo legibles por la comunidad.

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
- **Consecuencias**: no hay cobro ni gating de contenido. La clasificación automática se agregó en
  ADR-010; la invitación manual sigue existiendo.

## ADR-007 — Competencias Strong Fit con puntaje por posición

- **Fecha**: 2026-10-20
- **Contexto**: el MVP priorizó registro + progreso; la siguiente fase incorpora eventos
  competitivos con varias pruebas y divisiones.
- **Decisión**: modelar `competitions`, `competition_divisions`, `competition_events`,
  `competition_registrations` y `competition_results`. El puntaje **no se persiste**: se calcula en
  `CompetitionScoring` por prueba, ordenando según `score_direction`, asignando
  `(participantes - puesto + 1) × 10` y sumando. Resultado ausente = 0; desempate por primeros
  puestos y luego nombre.
- **Consecuencias**: el ranking es reproducible y auditable mientras los resultados no cambien. Si
  se necesita historial de puntajes (por ejemplo, tras validar o cerrar), habrá que persistirlos.
  La edición de resultados por el atleta queda limitada a competencias `LIVE`; la validación por
  admin es la siguiente deuda.

## ADR-008 — Inscripción y resultados con upsert

- **Fecha**: 2026-10-20
- **Contexto**: un atleta puede cambiar de división o corregir una marca; un insert ciego generaría
  conflictos por las restricciones únicas.
- **Decisión**: `POST /competition_registrations?on_conflict=competition_id,athlete_id` y
  `POST /competition_results?on_conflict=event_id,athlete_id` con
  `Prefer: resolution=merge-duplicates,return=representation`.
- **Consecuencias**: las operaciones son idempotentes y no requieren PATCH separados. Los campos
  omitidos no se borran (merge), así que limpiar `notes` requiere enviar `""` o modelar un PATCH.

## ADR-010 — Clasificación Ruky Elite por estándares

- **Fecha**: 2026-10-20
- **Contexto**: la división existía solo por invitación y la UI mostraba estándares de referencia.
- **Decisión**: `elite_standards` guarda 4 criterios configurables (Total SBD, Fran, HYROX y dominada
  lastrada). `EliteService` los evalúa sobre las últimas 52 semanas; cumplir 3 habilita
  `POST /api/elite/apply`, que usa `app_service` para actualizar `athletes.tier`.
- **Consecuencias**: las reglas son auditables y editables sin deploy. Los valores son una primera
  propuesta y deben validarse con el equipo; el atleta no puede editar su propio tier.
