package com.example.taskflow.postgrest;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskStatus;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * All PostgREST knowledge (paths, headers, column names, pagination) lives here.
 */
@Repository
public class PostgrestTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(PostgrestTaskRepository.class);
    private static final String TASKS = "/tasks";
    private static final String PREFER_REPRESENTATION = "return=representation";
    private static final String PREFER_COUNT = "count=exact";

    private final RestClient client;

    public PostgrestTaskRepository(RestClient client) {
        this.client = client;
    }

    public TaskPage findPage(TaskStatus status, int limit, int offset) {
        return translate(() -> {
            ResponseEntity<PostgrestTask[]> response = client.get()
                    .uri(uri -> {
                        uri.path(TASKS)
                                .queryParam("select", "*")
                                .queryParam("order", "created_at.desc")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (status != null) {
                            uri.queryParam("status", "eq." + status.name());
                        }
                        return uri.build();
                    })
                    .header("Prefer", PREFER_COUNT)
                    .retrieve()
                    .toEntity(PostgrestTask[].class);

            List<PostgrestTask> rows = response.getBody() == null
                    ? List.of()
                    : Arrays.asList(response.getBody());
            List<Task> items = rows.stream().map(PostgrestTask::toDomain).toList();
            long total = parseTotal(response.getHeaders(), items.size());
            return new TaskPage(items, total);
        });
    }

    public Optional<Task> findById(UUID id) {
        return translate(() -> {
            PostgrestTask[] rows = client.get()
                    .uri(uri -> uri.path(TASKS)
                            .queryParam("select", "*")
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .body(PostgrestTask[].class);
            return first(rows).map(PostgrestTask::toDomain);
        });
    }

    public Task create(TaskDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", draft.title());
        payload.put("description", draft.description());
        payload.put("status", draft.status().name());
        payload.put("priority", draft.priority().name());
        if (draft.dueDate() != null) {
            payload.put("due_date", draft.dueDate().toString());
        }

        return translate(() -> {
            PostgrestTask[] rows = client.post()
                    .uri(TASKS)
                    .header("Prefer", PREFER_REPRESENTATION)
                    .body(payload)
                    .retrieve()
                    .body(PostgrestTask[].class);
            return first(rows)
                    .map(PostgrestTask::toDomain)
                    .orElseThrow(() -> new PostgrestException(
                            HttpStatusCode.valueOf(502),
                            "PostgREST no devolvió la tarea creada",
                            null));
        });
    }

    public Optional<Task> update(UUID id, Map<String, Object> changes) {
        return translate(() -> {
            PostgrestTask[] rows = client.patch()
                    .uri(uri -> uri.path(TASKS)
                            .queryParam("id", "eq." + id)
                            .build())
                    .header("Prefer", PREFER_REPRESENTATION)
                    .body(changes)
                    .retrieve()
                    .body(PostgrestTask[].class);
            return first(rows).map(PostgrestTask::toDomain);
        });
    }

    public boolean delete(UUID id) {
        return translate(() -> {
            PostgrestTask[] rows = client.delete()
                    .uri(uri -> uri.path(TASKS)
                            .queryParam("id", "eq." + id)
                            .build())
                    .header("Prefer", PREFER_REPRESENTATION)
                    .retrieve()
                    .body(PostgrestTask[].class);
            return rows != null && rows.length > 0;
        });
    }

    private static Optional<PostgrestTask> first(PostgrestTask[] rows) {
        return rows == null || rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
    }

    private static long parseTotal(HttpHeaders headers, int fallback) {
        // Content-Range: 0-19/42 (or * / 0 when the page is empty)
        String header = headers.getFirst("Content-Range");
        if (header == null) {
            return fallback;
        }
        int slash = header.lastIndexOf('/');
        if (slash < 0) {
            return fallback;
        }
        String rawTotal = header.substring(slash + 1).trim();
        if (rawTotal.isEmpty() || "*".equals(rawTotal)) {
            return fallback;
        }
        try {
            return Long.parseLong(rawTotal);
        } catch (NumberFormatException ex) {
            log.warn("Unexpected Content-Range header: {}", header);
            return fallback;
        }
    }

    private static <T> T translate(UpstreamCall<T> call) {
        try {
            return call.execute();
        } catch (RestClientResponseException ex) {
            log.warn("PostgREST replied {} {}: {}",
                    ex.getStatusCode().value(),
                    ex.getStatusText(),
                    ex.getResponseBodyAsString());
            throw new PostgrestException(ex.getStatusCode(),
                    "El servicio de datos rechazó la operación", ex);
        } catch (ResourceAccessException ex) {
            log.warn("PostgREST is unreachable: {}", ex.getMessage());
            throw new PostgrestException(HttpStatusCode.valueOf(502),
                    "El servicio de datos no está disponible", ex);
        }
    }

    @FunctionalInterface
    private interface UpstreamCall<T> {
        T execute();
    }
}
