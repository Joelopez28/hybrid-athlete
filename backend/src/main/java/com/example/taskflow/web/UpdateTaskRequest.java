package com.example.taskflow.web;

import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Every field is optional: PATCH only sends what changed. Sending an empty
 * description clears it; sending null is treated as "not provided".
 */
public record UpdateTaskRequest(
        @Size(max = 200, message = "El título no puede superar 200 caracteres")
        String title,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String description,

        TaskStatus status,

        TaskPriority priority,

        @FutureOrPresent(message = "La fecha límite no puede ser pasada")
        LocalDate dueDate) {
}
