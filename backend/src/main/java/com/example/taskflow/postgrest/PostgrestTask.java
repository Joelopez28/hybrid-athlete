package com.example.taskflow.postgrest;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transport shape returned by PostgREST. It is the only place that knows about
 * snake_case columns; the rest of the application uses {@link Task}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostgrestTask(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        @JsonProperty("due_date") LocalDate dueDate,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {

    Task toDomain() {
        return new Task(id, title, description, status, priority, dueDate, createdAt, updatedAt);
    }
}
