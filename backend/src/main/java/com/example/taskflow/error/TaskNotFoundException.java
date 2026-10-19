package com.example.taskflow.error;

import java.util.UUID;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID id) {
        super("No existe la tarea " + id);
    }
}
