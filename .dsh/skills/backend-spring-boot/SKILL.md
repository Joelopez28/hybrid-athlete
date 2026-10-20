---
name: backend-spring-boot
description: Implementar y probar el BFF Spring Boot de RUKY (Java 17): endpoints, RPC atómica, estadísticas, PRs y ranking.
whenToUse: Al tocar controladores, servicios, repositorios PostgREST, cálculos de progreso o tests del backend.
---

# Backend Spring Boot (RUKY)

## Paquetes (`com.ruky.training`)

```text
config/      RestClient (mapper snake_case + JDK HttpClient), CORS, Clock
domain/      Athlete, Exercise, TrainingSession, SessionEntry, enums
postgrest/   Repositorios, SessionWrite/EntryWrite, SessionPage
service/     SessionService, StatsService, LeaderboardService, CurrentAthleteService
web/         Controllers, DTOs de request/response, PageResponse
error/       ApiError, GlobalExceptionHandler, ResourceNotFoundException
```

`web` no conoce `postgrest`; `service` orquesta; el repositorio es el único que sabe URLs/headers.

## Dos clientes distintos

1. **API pública**: Jackson camelCase (config global de Spring).
2. **PostgREST**: un `ObjectMapper` con `SNAKE_CASE` y `NON_NULL` inyectado en el `RestClient`. Por eso
   los records de transporte usan camelCase y PostgREST ve `performed_on`, `weight_kg`, etc.

No agregues anotaciones `@JsonProperty` sueltas: el naming ya está resuelto en `RestClientConfig`.

## Crear una sesión (flujo crítico)

PostgREST 12 **no** acepta nested inserts en este deployment. La sesión y sus series se crean con la
función `api.create_session(payload jsonb)`:

```java
UUID id = client.post()
    .uri("/rpc/create_session")
    .body(Map.of("payload", write))
    .retrieve()
    .body(UUID.class);
return findById(id).orElseThrow(...);
```

- El body de RPC va envuelto en `{"payload": {...}}`; sin el wrapper PostgREST busca una función sin
  argumentos y devuelve 404.
- `SessionWrite.entries` se serializa como `entries` (la función lo espera así).
- Si cambiás la firma de la función, actualizá `005_functions.sql`, `SessionWrite` y los tests.

## Reglas de negocio (no las dupliques en React)

- `STRENGTH`/`SKILL` exigen al menos una serie con reps o segundos.
- `WOD` exige formato y score; `HYROX_SIM` exige tiempo.
- El nombre del movimiento se denormaliza desde el catálogo (`exercise.name`) para que el historial no
  dependa de joins.
- Volumen: `Σ reps × weight_kg`; sin peso externo → 0.
- PRs: solo `STRENGTH`/`SKILL`; comparan peso, luego reps, luego segundos. Los WOD no generan PRs.
- `StatsService` trabaja sobre la ventana de semanas y devuelve buckets semanales desde el lunes.
- `LeaderboardService` agrupa sesiones por atleta; no N+1 a la base.

## Competencias

`CompetitionService` carga competencia + divisiones + eventos + inscripciones + resultados y delega el
puntaje en `CompetitionScoring`:

- Por prueba: ordenar por `raw_score` según `score_direction`; puntos = `(participantes - puesto + 1) × 10`.
- Ausente = 0 puntos; desempate por total, luego primeros puestos, luego nombre.
- `POST /register` y `POST /events/{id}/result` son upserts (`on_conflict`), no inserts ciegos.
- `Ruky Elite` exige `athlete.tier == ELITE`; solo se opera si `status == LIVE`.
- `displayScore` se autogenera para TIME (mm:ss), WEIGHT (kg) y REPS si el cliente no lo manda.

Tests: `CompetitionScoringTest` (puro), `CompetitionServiceTest` (reglas), `CompetitionRepositoryTest`
(upsert/embed), `CompetitionControllerTest`.

## Errores

`GlobalExceptionHandler` mapea a `{timestamp,status,error,message,path}`:

| Excepción | HTTP |
|---|---|
| `MethodArgumentNotValidException` | 400 |
| `IllegalArgumentException` | 400 |
| `ResourceNotFoundException` | 404 |
| `PostgrestException` 409 | 409 |
| `PostgrestException` / red | 502 |
| `Exception` | 500 (sin internals) |

Nunca devuelvas el body crudo de PostgREST ni un stacktrace.

## Tests

- **Servicio**: Mockito puro. Cubrí defaults, validaciones condicionales, ownership, PRs y holds.
- **Controller**: `@WebMvcTest` + `@MockBean SessionService`; 201+Location, 400, 404, 204.
- **Repositorio**: `MockRestServiceServer.bindTo(RestClient.Builder)` con el mismo mapper snake_case;
  verificá query params, `Prefer: count=exact`, RPC `$.payload`, orden de series y parseo de
  `Content-Range`.
- **Contexto**: `@SpringBootTest` con la configuración por defecto (no conecta a PostgREST al arrancar).

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw spring-boot:run
```

## Pitfalls que ya nos costaron tiempo

1. `SimpleClientHttpRequestFactory` no puede PATCH; usá `JdkClientHttpRequestFactory`.
2. RPC sin `{"payload": ...}` → 404 “function does not exist”.
3. Columna boolean `benchmark`, no `is_benchmark` (Jackson no la mapea).
4. `BigDecimal` para pesos/RPE; nunca `double`.
5. Ordenar series por `movement` y luego `setIndex` para agrupar en la UI.
6. No hay auth: **no** aceptes `athleteId` desde el request; sale de `CurrentAthleteService`.
7. `StatsService` y `CompetitionScoring` recalculan en memoria; si crece el volumen, mover a vistas SQL.
8. En upsert, los campos omitidos **no** se borran (merge); para limpiar notas enviá `""` o modelá PATCH.

## Definition of done

- [ ] Contrato actualizado en `frontend/src/api/types.ts`.
- [ ] Validación + error shape.
- [ ] Tests de servicio y controller (y repositorio si tocaste queries).
- [ ] `./mvnw test` verde y `make smoke` si tocaste el flujo de sesiones.
