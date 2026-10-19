package com.example.taskflow.postgrest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskPriority;
import com.example.taskflow.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PostgrestTaskRepositoryTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TASK_JSON = """
            [{
              "id": "%s",
              "title": "Preparar demo",
              "description": null,
              "status": "TODO",
              "priority": "HIGH",
              "due_date": "2026-12-01",
              "created_at": "2026-10-18T12:00:00Z",
              "updated_at": "2026-10-18T12:00:00Z"
            }]
            """.formatted(ID);

    private MockRestServiceServer server;
    private PostgrestTaskRepository repository;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://postgrest.test");
        server = MockRestServiceServer.bindTo(builder).build();
        repository = new PostgrestTaskRepository(builder.build());
    }

    @Test
    void shouldReadPageAndContentRange() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", "0-0/3");
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("select", "*"))
                .andExpect(queryParam("order", "created_at.desc"))
                .andExpect(queryParam("limit", "20"))
                .andExpect(queryParam("offset", "0"))
                .andExpect(queryParam("status", "eq.TODO"))
                .andExpect(header("Prefer", "count=exact"))
                .andRespond(withSuccess(TASK_JSON, MediaType.APPLICATION_JSON).headers(headers));

        TaskPage page = repository.findPage(TaskStatus.TODO, 20, 0);

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.totalPages(20)).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(task -> {
            assertThat(task.id()).isEqualTo(ID);
            assertThat(task.dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(task.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(task.createdAt()).isNotNull();
        });
        server.verify();
    }

    @Test
    void shouldFallBackToItemCountWhenContentRangeIsMissing() {
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andRespond(withSuccess(TASK_JSON, MediaType.APPLICATION_JSON));

        TaskPage page = repository.findPage(null, 20, 0);

        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyWhenNoRowMatchesId() {
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andExpect(queryParam("id", "eq." + ID))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(repository.findById(ID)).isEmpty();
        server.verify();
    }

    @Test
    void shouldSendSnakeCasePayloadOnCreate() {
        server.expect(requestTo("http://postgrest.test/tasks"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Prefer", "return=representation"))
                .andExpect(jsonPath("$.title").value("Preparar demo"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.due_date").value("2026-12-01"))
                .andRespond(withSuccess(TASK_JSON, MediaType.APPLICATION_JSON));

        Task created = repository.create(new TaskDraft(
                "Preparar demo", null, TaskStatus.TODO, TaskPriority.HIGH, LocalDate.of(2026, 12, 1)));

        assertThat(created.title()).isEqualTo("Preparar demo");
        server.verify();
    }

    @Test
    void shouldReturnTrueWhenDeleteRemovesARow() {
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Prefer", "return=representation"))
                .andRespond(withSuccess(TASK_JSON, MediaType.APPLICATION_JSON));

        assertThat(repository.delete(ID)).isTrue();
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenDeleteMatchesNothing() {
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(repository.delete(ID)).isFalse();
    }

    @Test
    void shouldTranslateUpstreamErrors() {
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":"PGRST100","message":"failed to parse filter"}
                                """));

        assertThatThrownBy(() -> repository.findById(ID))
                .isInstanceOf(PostgrestException.class)
                .hasMessageContaining("servicio de datos");
    }

    @Test
    void shouldSendOnlyProvidedFieldsOnUpdate() {
        server.expect(requestTo(startsWith("http://postgrest.test/tasks?")))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.title").doesNotExist())
                .andRespond(withSuccess(TASK_JSON, MediaType.APPLICATION_JSON));

        repository.update(ID, Map.of("status", "DONE"));

        server.verify();
    }
}
