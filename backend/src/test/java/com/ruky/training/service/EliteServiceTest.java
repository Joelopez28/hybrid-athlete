package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.EliteStandard;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.Sex;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.postgrest.EliteRepository;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.web.EliteStatusResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EliteServiceTest {

    private static final UUID ATHLETE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private EliteRepository repository;

    @Mock
    private SessionRepository sessions;

    @Mock
    private CurrentAthleteService currentAthlete;

    private EliteService service;

    private final Athlete open = athlete(ATHLETE_ID, AthleteTier.OPEN);
    private final Athlete elite = athlete(ATHLETE_ID, AthleteTier.ELITE);
    private final List<EliteStandard> standards = List.of(
            new EliteStandard("PL_TOTAL", Discipline.POWERLIFTING, "Total SBD", "TOTAL_RATIO",
                    new BigDecimal("4.5"), "× peso corporal", null, true),
            new EliteStandard("CF_FRAN", Discipline.CROSSFIT, "Fran", "FRAN_TIME",
                    new BigDecimal("270"), "segundos", null, true),
            new EliteStandard("HYROX_OPEN", Discipline.HYROX, "HYROX", "HYROX_TIME",
                    new BigDecimal("5100"), "segundos", null, true),
            new EliteStandard("CAL_PULLUP", Discipline.CALISTHENICS, "Dominada lastrada",
                    "PULLUP_RATIO", new BigDecimal("0.20"), "× peso corporal", null, true));

    @BeforeEach
    void setUp() {
        service = new EliteService(repository, sessions, currentAthlete,
                Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));
        lenient().when(repository.findStandards()).thenReturn(standards);
    }

    @Test
    void shouldQualifyWhenThreeStandardsAreMet() {
        when(currentAthlete.get()).thenReturn(open);
        when(sessions.findAllSince(eq(ATHLETE_ID), any(LocalDate.class))).thenReturn(history());

        EliteStatusResponse status = service.status();

        assertThat(status.qualified()).isTrue();
        assertThat(status.metCount()).isEqualTo(4);
        assertThat(status.requiredCount()).isEqualTo(3);
        assertThat(status.standards()).extracting(criterion -> criterion.code())
                .containsExactly("PL_TOTAL", "CF_FRAN", "HYROX_OPEN", "CAL_PULLUP");
        assertThat(status.message()).contains("solicitar");
    }

    @Test
    void shouldNotQualifyWithoutThreeStandards() {
        when(currentAthlete.get()).thenReturn(open);
        when(sessions.findAllSince(eq(ATHLETE_ID), any(LocalDate.class))).thenReturn(List.of(
                strength(ATHLETE_ID, "Back Squat", "130"),
                strength(ATHLETE_ID, "Bench Press", "100"),
                strength(ATHLETE_ID, "Deadlift", "170")));

        EliteStatusResponse status = service.status();

        assertThat(status.qualified()).isFalse();
        assertThat(status.metCount()).isEqualTo(1);
        assertThatThrownBy(service::apply).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldPromoteQualifiedOpenAthlete() {
        when(currentAthlete.get()).thenReturn(open, elite);
        when(sessions.findAllSince(eq(ATHLETE_ID), any(LocalDate.class))).thenReturn(history());
        when(repository.promote(ATHLETE_ID)).thenReturn(true);

        EliteStatusResponse status = service.apply();

        verify(repository).promote(ATHLETE_ID);
        assertThat(status.tier()).isEqualTo(AthleteTier.ELITE);
    }

    private List<TrainingSession> history() {
        return List.of(
                strength(ATHLETE_ID, "Back Squat", "130"),
                strength(ATHLETE_ID, "Bench Press", "100"),
                strength(ATHLETE_ID, "Deadlift", "170"),
                wods(ATHLETE_ID, "WOD \"Fran\"", Discipline.CROSSFIT, SessionType.WOD, "04:20"),
                wods(ATHLETE_ID, "HYROX sim", Discipline.HYROX, SessionType.HYROX_SIM, "1:18:40"),
                skill(ATHLETE_ID, "Weighted Pull-up", "20"));
    }

    private static Athlete athlete(UUID id, AthleteTier tier) {
        return new Athlete(id, "ruky", "Ruky", Sex.MALE, null, new BigDecimal("82"), "CL", null,
                tier, List.of());
    }

    private static TrainingSession strength(UUID athleteId, String movement, String weight) {
        return new TrainingSession(UUID.randomUUID(), athleteId, LocalDate.of(2026, 10, 20),
                Discipline.POWERLIFTING, SessionType.STRENGTH, "Fuerza", null, 60, null,
                null, null, null, OffsetDateTime.now(), List.of(new SessionEntry(
                UUID.randomUUID(), UUID.randomUUID(), movement, 1, 1, new BigDecimal(weight),
                null, null)));
    }

    private static TrainingSession wods(UUID athleteId, String title, Discipline discipline,
                                        SessionType type, String score) {
        return new TrainingSession(UUID.randomUUID(), athleteId, LocalDate.of(2026, 10, 20),
                discipline, type, title, null, 20, null, null, score, null,
                OffsetDateTime.now(), List.of());
    }

    private static TrainingSession skill(UUID athleteId, String movement, String weight) {
        return new TrainingSession(UUID.randomUUID(), athleteId, LocalDate.of(2026, 10, 20),
                Discipline.CALISTHENICS, SessionType.SKILL, "Skills", null, 30, null,
                null, null, null, OffsetDateTime.now(), List.of(new SessionEntry(
                UUID.randomUUID(), UUID.randomUUID(), movement, 1, 5, new BigDecimal(weight),
                null, null)));
    }
}
