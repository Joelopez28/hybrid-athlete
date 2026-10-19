package com.example.taskflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.postgrest.PostgrestTaskRepository;
import com.example.taskflow.postgrest.TaskDraft;
import com.example.taskflow.web.CreateTaskRequest;
import com.example.taskflow.web.UpdateTaskRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private PostgrestTaskRepository repository;

    @InjectMocks
    private TaskService service;

    @Test
    void shouldApplyDefaultsWhenCreating() {
        when(repository.create(any(TaskDraft.class))).thenAnswer(invocation -> {
            TaskDraft draft = invocation.getArgument(0);
            return new Task(ID, draft.title(), draft.description(), draft.status(),
                    draft.priority(), draft.dueDate(), OffsetDateTime.now(), OffsetDateTime.now());
        });

        Task created = service.create(new CreateTaskRequest("  Comprar café  ", "  ", null, null, null));

        ArgumentCaptor<TaskDraft> captor = ArgumentCaptor.forClass(TaskDraft.class);
        verify(repository).create(captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("Comprar café");
        assertThat(captor.getValue().description()).isNull();
        assertThat(captor.getValue().status()).isEqualTo(TaskStatus.TODO);
        assertThat(captor.getValue().priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(created.title()).isEqualTo("Comprar café");
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThatThrownBy(() -> service.create(new CreateTaskRequest("   ", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("título");
    }

    @Test
    void shouldRejectEmptyPatch() {
        assertThatThrownBy(() -> service.update(ID, new UpdateTaskRequest(null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un campo");
    }

    @Test
    void shouldMapPatchFieldsToSnakeCaseColumns() {
        when(repository.update(eq(ID), anyMap())).thenReturn(Optional.of(
                new Task(ID, "Nueva", null, TaskStatus.DONE, TaskPriority.HIGH, null,
                        OffsetDateTime.now(), OffsetDateTime.now())));

        service.update(ID, new UpdateTaskRequest("Nueva", "", TaskStatus.DONE, TaskPriority.HIGH, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repository).update(eq(ID), captor.capture());
        assertThat(captor.getValue())
                .containsEntry("title", "Nueva")
                .containsEntry("description", null)
                .containsEntry("status", "DONE")
                .containsEntry("priority", "HIGH")
                .doesNotContainKey("due_date");
    }

    @Test
    void shouldRejectOutOfRangePagination() {
        assertThatThrownBy(() -> service.list(null, 0, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
        assertThatThrownBy(() -> service.list(null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    void shouldThrowWhenTaskDoesNotExist() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ID)).hasMessageContaining(ID.toString());
    }
}
