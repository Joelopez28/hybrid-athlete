package com.example.taskflow.web;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede superar 200 caracteres")
        String title,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String description,

        TaskStatus status,

        TaskPriority priority,

        @FutureOrPresent(message = "La fecha límite no puede ser pasada")
        LocalDate dueDate) {
}
