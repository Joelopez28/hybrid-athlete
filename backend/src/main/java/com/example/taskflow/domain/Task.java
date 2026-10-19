package com.example.taskflow.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain model. Camel-case everywhere; the snake_case mapping to PostgREST
 * lives only in the transport record.
 */
public record Task(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
