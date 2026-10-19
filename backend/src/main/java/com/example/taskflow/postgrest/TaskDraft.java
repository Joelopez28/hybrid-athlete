package com.example.taskflow.postgrest;

import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import java.time.LocalDate;

public record TaskDraft(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {
}
