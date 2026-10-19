package com.example.taskflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.error.TaskNotFoundException;
import com.example.taskflow.postgrest.TaskPage;
import com.example.taskflow.service.TaskService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Task TASK = new Task(
            ID, "Preparar demo", "Mostrar el flujo completo", TaskStatus.TODO, TaskPriority.HIGH,
            LocalDate.of(2026, 12, 1), OffsetDateTime.parse("2026-10-18T12:00:00Z"),
            OffsetDateTime.parse("2026-10-18T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService service;

    @Test
    void shouldListTasksWithPaginationMetadata() throws Exception {
        when(service.list(null, 0, 20)).thenReturn(new TaskPage(List.of(TASK), 1));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Preparar demo"))
                .andExpect(jsonPath("$.items[0].dueDate").value("2026-12-01"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldCreateTaskAndReturnLocation() throws Exception {
        when(service.create(any(CreateTaskRequest.class))).thenReturn(TASK);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Preparar demo","priority":"HIGH","dueDate":"2026-12-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("título")))
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    @Test
    void shouldReturnNotFoundForUnknownTask() throws Exception {
        when(service.get(ID)).thenThrow(new TaskNotFoundException(ID));

        mockMvc.perform(get("/api/tasks/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/" + ID));
    }

    @Test
    void shouldPatchTask() throws Exception {
        Task done = new Task(ID, TASK.title(), TASK.description(), TaskStatus.DONE, TASK.priority(),
                TASK.dueDate(), TASK.createdAt(), TASK.updatedAt());
        when(service.update(eq(ID), any(UpdateTaskRequest.class))).thenReturn(done);

        mockMvc.perform(patch("/api/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void shouldDeleteTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new TaskNotFoundException(ID)).when(service).delete(ID);

        mockMvc.perform(delete("/api/tasks/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "DOING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));
    }
}
