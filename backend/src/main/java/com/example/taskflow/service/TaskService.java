package com.example.taskflow.service;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.error.TaskNotFoundException;
import com.example.taskflow.postgrest.PostgrestTaskRepository;
import com.example.taskflow.postgrest.TaskDraft;
import com.example.taskflow.postgrest.TaskPage;
import com.example.taskflow.web.CreateTaskRequest;
import com.example.taskflow.web.UpdateTaskRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 10_000;

    private final PostgrestTaskRepository repository;

    public TaskService(PostgrestTaskRepository repository) {
        this.repository = repository;
    }

    public TaskPage list(TaskStatus status, int page, int size) {
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException("page debe estar entre 0 y " + MAX_PAGE);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
        return repository.findPage(status, size, page * size);
    }

    public Task get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task create(CreateTaskRequest request) {
        TaskDraft draft = new TaskDraft(
                requiredTitle(request.title()),
                optionalText(request.description()),
                request.status() == null ? TaskStatus.TODO : request.status(),
                request.priority() == null ? TaskPriority.MEDIUM : request.priority(),
                request.dueDate());
        return repository.create(draft);
    }

    public Task update(UUID id, UpdateTaskRequest request) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (request.title() != null) {
            changes.put("title", requiredTitle(request.title()));
        }
        if (request.description() != null) {
            changes.put("description", optionalText(request.description()));
        }
        if (request.status() != null) {
            changes.put("status", request.status().name());
        }
        if (request.priority() != null) {
            changes.put("priority", request.priority().name());
        }
        if (request.dueDate() != null) {
            changes.put("due_date", request.dueDate().toString());
        }
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("Enviá al menos un campo para actualizar");
        }
        return repository.update(id, changes).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public void delete(UUID id) {
        if (!repository.delete(id)) {
            throw new TaskNotFoundException(id);
        }
    }

    private static String requiredTitle(String value) {
        String title = value == null ? "" : value.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("El título no puede superar 200 caracteres");
        }
        return title;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
