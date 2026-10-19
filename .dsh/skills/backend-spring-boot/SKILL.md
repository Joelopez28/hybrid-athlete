---
name: backend-spring-boot
description: Implementar y probar el BFF Spring Boot 3 (Java 17) que consume PostgREST. DTOs, validación, RestClient, errores y tests.
whenToUse: Al crear o modificar endpoints /api, clientes PostgREST, validaciones, manejo de errores o tests del backend.
---

# Backend Spring Boot (BFF)

## Mapa de paquetes

```text
com.example.taskflow
├── TaskFlowApplication.java
├── config/        PostgrestProperties, RestClientConfig, WebConfig (CORS)
├── domain/        Task, TaskStatus, TaskPriority (records/enums, camelCase)
├── postgrest/     PostgrestTaskRepository, PostgrestException, mapeo snake_case
├── service/       TaskService (reglas de negocio)
├── web/           TaskController, DTOs de request/response
└── error/         ApiError, GlobalExceptionHandler, TaskNotFoundException
```

Regla: **`web` no conoce `postgrest`**; `service` es la única capa que puede combinar repositorios.
El repositorio es el único lugar con URLs y headers de PostgREST.

## RestClient (Spring 6.1, sin WebFlux)

```java
@Configuration
class RestClientConfig {
  @Bean
  RestClient postgrestRestClient(PostgrestProperties props) {
    var httpClient = HttpClient.newBuilder().connectTimeout(props.connectTimeout()).build();
    var factory = new JdkClientHttpRequestFactory(httpClient); // PATCH-capable
    factory.setReadTimeout(props.readTimeout());
    return RestClient.builder()
        .baseUrl(props.baseUrl())
        .requestFactory(factory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
```

No uses `SimpleClientHttpRequestFactory`: sobre `HttpURLConnection` **no puede enviar PATCH**
(`Invalid HTTP method: PATCH` → 502). Ese bug ya lo pagamos; el smoke lo detecta.

Config por entorno (`application.yml`): `app.postgrest.base-url: ${POSTGREST_URL:http://localhost:3000}`.
Validá la URL al arrancar (`@Validated` + `@NotBlank`); nunca hardcodees el host.

## Patrón de repositorio PostgREST

```java
List<Task> findAll(TaskStatus status, int limit, int offset) {
  var spec = restClient.get().uri(b -> {
    b.path("/tasks").queryParam("select", "*")
     .queryParam("order", "created_at.desc")
     .queryParam("limit", limit).queryParam("offset", offset);
    if (status != null) b.queryParam("status", "eq." + status.name());
    return b.build();
  });
  var response = spec.header("Prefer", "count=exact").retrieve().toEntity(Task[].class);
  // Content-Range: 0-19/42  → total = 42
  return List.of(Objects.requireNonNull(response.getBody()));
}
```

- Escapá valores de filtro (`URLEncoder`/builder) y validá enums antes de concatenar.
- `POST`/`PATCH` con `Prefer: return=representation`; `DELETE` espera 204.
- PostgREST devuelve **array** incluso para búsquedas por id: tomá el primero y lanzá `TaskNotFoundException`.
- 404 de PostgREST no siempre significa "no existe": revisá el body (`code: PGRST116`).
- Errores HTTP: capturá `RestClientResponseException`, parseá `{message, code, details, hint}` y
  traducí a excepciones propias. Nunca propagues el body crudo al navegador.

## Contrato y validación

```java
public record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    TaskStatus status,                 // null → TODO
    TaskPriority priority,             // null → MEDIUM
    @FutureOrPresent LocalDate dueDate) {}
```

- `@Valid` en el controller; errores de campo → 400 con detalle legible.
- `PATCH`: todos los campos opcionales; si no viene ninguno → 400 (`IllegalArgumentException`).
- No aceptes `id`, `createdAt`, `updatedAt` desde el cliente. El `id` va en la URL.
- Fechas: `OffsetDateTime` para timestamps, `LocalDate` para fechas sin hora. Jackson con `JavaTimeModule`.

## Manejo de errores

`@RestControllerAdvice` con un único shape (`ApiError`) y `ProblemDetail`-style. Mapeo mínimo:

| Excepción | HTTP |
|---|---|
| `MethodArgumentNotValidException` | 400 |
| `IllegalArgumentException` | 400 |
| `TaskNotFoundException` | 404 |
| `PostgrestException` (4xx de negocio) | 409/422 |
| `ResourceAccessException` / 5xx PostgREST | 502 |

Logueá el detalle técnico con `log.warn` y devolvé un mensaje accionable sin internals.

## Tests (rápidos, sin DB)

- **Controller**: `@WebMvcTest(TaskController.class)` + `@MockBean TaskService`; validá 201/400/404 y
  el shape de error. No levantes el contexto completo para esto.
- **Repositorio**: `MockRestServiceServer.bindTo(restClientBuilder)`; verificá path, query params,
  header `Prefer` y parseo de `Content-Range`. Es la prueba que detecta el 90% de las roturas.
- **Context**: un `@SpringBootTest` liviano con `app.postgrest.base-url=http://localhost:1` (no conecta
  al arrancar).
- Nombres: `shouldRejectBlankTitle`, `shouldMapContentRangeToTotal`. Un assert por comportamiento.

```bash
./mvnw test                 # o mvn test
./mvnw -q -DskipTests package
./mvnw spring-boot:run      # :8080
```

## Pitfalls que ya nos costaron tiempo

1. `RestClient` no lanza excepción en 4xx por defecto si usás `exchange`; preferí `retrieve()`.
2. `PATCH` con `null` explícito borra la columna; construí el mapa solo con campos presentes
   (`JsonInclude(NON_NULL)` no alcanza si el record trae null). Usá `Optional`/mapa explícito.
3. `Content-Range` ausente cuando PostgREST no cuenta (`Prefer` mal escrito) → total = -1.
4. `@FutureOrPresent` sobre `LocalDate` usa el reloj del servidor; testealo con fechas relativas.
5. CORS: en dev el frontend usa el proxy de Vite; igual configurá `allowedOrigins` por propiedad.
6. No agregues `spring-boot-starter-data-jpa`; el acceso a datos es HTTP.

## Definition of done

- [ ] Endpoint documentado en README y tipado en `frontend/src/api/types.ts`.
- [ ] Validación de entrada + error shape uniforme.
- [ ] Tests de controller y repositorio cubriendo el caso feliz y el error principal.
- [ ] `./mvnw test` verde; sin warnings nuevos de compilación.
