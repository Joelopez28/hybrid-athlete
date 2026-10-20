package com.ruky.training.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruky.training.domain.CompetitionStatus;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.service.CompetitionService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompetitionController.class)
class CompetitionControllerTest {

    private static final String SLUG = "strong-fit-ruky-1";
    private static final UUID DIVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final CompetitionSummaryResponse SUMMARY = new CompetitionSummaryResponse(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), SLUG, "Strong Fit RUKY #1",
            "Cuatro pruebas", CompetitionStatus.LIVE, LocalDate.of(2026, 10, 18),
            LocalDate.of(2026, 10, 25), "Santiago");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompetitionService service;

    @Test
    void shouldListCompetitions() throws Exception {
        when(service.list()).thenReturn(List.of(SUMMARY));

        mockMvc.perform(get("/api/competitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value(SLUG))
                .andExpect(jsonPath("$[0].status").value("LIVE"));
    }

    @Test
    void shouldReturnDetail() throws Exception {
        CompetitionDetailResponse detail = new CompetitionDetailResponse(
                SUMMARY, List.of(), List.of(), null, List.of(), List.of());
        when(service.detail(SLUG)).thenReturn(detail);

        mockMvc.perform(get("/api/competitions/{slug}", SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competition.name").value("Strong Fit RUKY #1"));
    }

    @Test
    void shouldRegisterAndWithdraw() throws Exception {
        mockMvc.perform(post("/api/competitions/{slug}/register", SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"divisionId\":\"" + DIVISION_ID + "\"}"))
                .andExpect(status().isNoContent());
        verify(service).register(eq(SLUG), any(RegisterRequest.class));

        mockMvc.perform(delete("/api/competitions/{slug}/register", SLUG))
                .andExpect(status().isNoContent());
        verify(service).withdraw(SLUG);
    }

    @Test
    void shouldRejectResultWithoutScore() throws Exception {
        mockMvc.perform(post("/api/competitions/{slug}/events/{eventId}/result", SLUG, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("resultado")));
    }

    @Test
    void shouldReturnNotFoundForUnknownCompetition() throws Exception {
        when(service.detail("nope")).thenThrow(new ResourceNotFoundException("No existe"));

        mockMvc.perform(get("/api/competitions/{slug}", "nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenRegistrationFails() throws Exception {
        doThrow(new IllegalArgumentException("La división está completa"))
                .when(service).register(eq(SLUG), any());

        mockMvc.perform(post("/api/competitions/{slug}/register", SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"divisionId\":\"" + DIVISION_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La división está completa"));
    }
}
