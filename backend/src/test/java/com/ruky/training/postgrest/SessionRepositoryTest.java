package com.ruky.training.postgrest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SessionRepositoryTest {

    private static final UUID ATHLETE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SESSION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID EXERCISE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final String SESSION_JSON = """
            [{
              "id": "%s",
              "athlete_id": "%s",
              "performed_on": "2026-10-20",
              "discipline": "POWERLIFTING",
              "session_type": "STRENGTH",
              "title": "Fuerza",
              "duration_min": 60,
              "created_at": "2026-10-20T12:00:00Z",
              "session_entries": [
                {"id": "11111111-1111-1111-1111-111111111111", "exercise_id": "%s",
                 "movement": "Back Squat", "set_index": 2, "reps": 3, "weight_kg": 110},
                {"id": "22222222-2222-2222-2222-222222222222", "exercise_id": "%s",
                 "movement": "Back Squat", "set_index": 1, "reps": 5, "weight_kg": 100}
              ]
            }]
            """.formatted(SESSION_ID, ATHLETE_ID, EXERCISE_ID, EXERCISE_ID);

    private MockRestServiceServer server;
    private SessionRepository repository;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://postgrest.test")
                .messageConverters(converters ->
                        converters.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        repository = new SessionRepository(builder.build());
    }

    @Test
    void shouldReadPageAndSortEntriesByMovementAndSet() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", "0-0/5");
        server.expect(requestTo(startsWith("http://postgrest.test/training_sessions?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("athlete_id", "eq." + ATHLETE_ID))
                .andExpect(queryParam("select", "*,session_entries(*)"))
                .andExpect(header("Prefer", "count=exact"))
                .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON).headers(headers));

        SessionPage page = repository.findPage(ATHLETE_ID, Discipline.POWERLIFTING, null, 20, 0);

        assertThat(page.total()).isEqualTo(5);
        assertThat(page.items()).singleElement().satisfies(session -> {
            assertThat(session.title()).isEqualTo("Fuerza");
            assertThat(session.volumeKg()).isEqualByComparingTo("830");
            assertThat(session.entries()).extracting(entry -> entry.setIndex()).containsExactly(1, 2);
        });
        server.verify();
    }

    @Test
    void shouldCreateThroughRpcAndReadBack() {
        server.expect(requestTo("http://postgrest.test/rpc/create_session"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.payload.title").value("Fuerza"))
                .andExpect(jsonPath("$.payload.entries[0].movement").value("Back Squat"))
                .andRespond(withSuccess("\"" + SESSION_ID + "\"", MediaType.APPLICATION_JSON));

        server.expect(requestTo(startsWith("http://postgrest.test/training_sessions?")))
                .andExpect(queryParam("id", "eq." + SESSION_ID))
                .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));

        SessionWrite write = new SessionWrite(ATHLETE_ID, LocalDate.of(2026, 10, 20),
                Discipline.POWERLIFTING, SessionType.STRENGTH, "Fuerza", null, 60, null,
                null, null, null,
                List.of(new EntryWrite(EXERCISE_ID, "Back Squat", 1, 5, new BigDecimal("100"), null, null)));

        TrainingSession created = repository.create(write);

        assertThat(created.id()).isEqualTo(SESSION_ID);
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenDeleteMatchesNothing() {
        server.expect(requestTo(startsWith("http://postgrest.test/training_sessions?")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Prefer", "return=representation"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(repository.delete(SESSION_ID)).isFalse();
    }

    @Test
    void shouldFindByIdWithEmbeddedEntries() {
        server.expect(requestTo(startsWith("http://postgrest.test/training_sessions?")))
                .andExpect(queryParam("id", "eq." + SESSION_ID))
                .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));

        assertThat(repository.findById(SESSION_ID)).get()
                .satisfies(session -> assertThat(session.entries()).hasSize(2));
    }
}
