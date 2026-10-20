package com.ruky.training.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.postgrest.SessionPage;
import com.ruky.training.service.SessionService;
import java.math.BigDecimal;
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

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    private static final UUID ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final TrainingSession SESSION = new TrainingSession(ID,
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), LocalDate.of(2026, 10, 20),
            Discipline.POWERLIFTING, SessionType.STRENGTH, "Fuerza", "Notas", 60,
            new BigDecimal("8.0"), null, null, null,
            OffsetDateTime.parse("2026-10-20T12:00:00Z"),
            List.of(new SessionEntry(UUID.randomUUID(), UUID.randomUUID(), "Back Squat", 1, 5,
                    new BigDecimal("100.0"), null, null)));

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionService service;

    @Test
    void shouldListSessionsWithPagination() throws Exception {
        when(service.list(null, null, 0, 20)).thenReturn(new SessionPage(List.of(SESSION), 1));

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Fuerza"))
                .andExpect(jsonPath("$.items[0].volumeKg").value(500.0))
                .andExpect(jsonPath("$.items[0].entries[0].movement").value("Back Squat"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldCreateSessionAndReturnLocation() throws Exception {
        when(service.create(any(CreateSessionRequest.class))).thenReturn(SESSION);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "performedOn": "2026-10-20",
                                  "discipline": "POWERLIFTING",
                                  "sessionType": "STRENGTH",
                                  "title": "Fuerza",
                                  "durationMin": 60,
                                  "entries": [{"movement": "Back Squat", "reps": 5, "weightKg": 100}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/sessions/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void shouldRejectBlankTitle() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "performedOn": "2026-10-20",
                                  "discipline": "POWERLIFTING",
                                  "sessionType": "STRENGTH",
                                  "title": "  "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("título")));
    }

    @Test
    void shouldRejectFutureDate() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "performedOn": "2099-01-01",
                                  "discipline": "POWERLIFTING",
                                  "sessionType": "STRENGTH",
                                  "title": "Futuro"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("futura")));
    }

    @Test
    void shouldDeleteSession() throws Exception {
        mockMvc.perform(delete("/api/sessions/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingUnknownSession() throws Exception {
        doThrow(new ResourceNotFoundException("No existe la sesión " + ID))
                .when(service).delete(eq(ID));

        mockMvc.perform(delete("/api/sessions/{id}", ID))
                .andExpect(status().isNotFound());
    }
}
