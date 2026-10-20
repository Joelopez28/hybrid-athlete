package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.Sex;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.web.StatsResponse;
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
class StatsServiceTest {

    private static final UUID ATHLETE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SQUAT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BENCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private SessionRepository sessions;

    @Mock
    private CurrentAthleteService currentAthlete;

    private StatsService service;

    private final Athlete athlete = new Athlete(ATHLETE_ID, "ruky", "Ruky", Sex.MALE, null,
            new BigDecimal("82.0"), "CL", null, AthleteTier.ELITE, List.of(Discipline.POWERLIFTING));

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
        service = new StatsService(sessions, currentAthlete, clock);
        lenient().when(currentAthlete.get()).thenReturn(athlete);
    }

    @Test
    void shouldAggregateWeeklyVolumeAndPersonalRecords() {
        when(sessions.findAllSince(eq(ATHLETE_ID), any(LocalDate.class))).thenReturn(List.of(
                session(LocalDate.of(2026, 10, 13), Discipline.POWERLIFTING, 45,
                        entry(SQUAT_ID, "Back Squat", 3, "110")),
                session(LocalDate.of(2026, 10, 19), Discipline.POWERLIFTING, 60,
                        entry(SQUAT_ID, "Back Squat", 5, "100"),
                        entry(BENCH_ID, "Bench Press", 5, "80")),
                session(LocalDate.of(2026, 10, 20), Discipline.CROSSFIT, 20)));

        StatsResponse stats = service.get(4);

        assertThat(stats.totals().sessions()).isEqualTo(3);
        assertThat(stats.totals().minutes()).isEqualTo(125);
        assertThat(stats.totals().volumeKg()).isEqualByComparingTo("1230.0");
        assertThat(stats.totals().prs()).isEqualTo(2);

        assertThat(stats.weekly()).hasSize(4);
        assertThat(stats.weekly().get(0).weekStart()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(stats.weekly().get(2).sessions()).isEqualTo(1);
        assertThat(stats.weekly().get(2).volumeKg()).isEqualByComparingTo("330.0");
        assertThat(stats.weekly().get(3).sessions()).isEqualTo(2);
        assertThat(stats.weekly().get(3).minutes()).isEqualTo(80);

        assertThat(stats.personalRecords()).hasSize(2);
        assertThat(stats.personalRecords().get(0).movement()).isEqualTo("Back Squat");
        assertThat(stats.personalRecords().get(0).bestWeightKg()).isEqualByComparingTo("110");
        assertThat(stats.personalRecords().get(1).movement()).isEqualTo("Bench Press");

        assertThat(stats.disciplines()).hasSize(2);
        assertThat(stats.disciplines().get(0).discipline()).isEqualTo(Discipline.POWERLIFTING);
        assertThat(stats.disciplines().get(0).sessions()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidWeekWindow() {
        assertThatThrownBy(() -> service.get(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weeks");
    }

    @Test
    void shouldRecordHoldSecondsAsAPersonalRecord() {
        SessionEntry hold = new SessionEntry(UUID.randomUUID(), UUID.randomUUID(),
                "Handstand Hold", 1, null, null, new BigDecimal("45"), null);
        when(sessions.findAllSince(eq(ATHLETE_ID), any(LocalDate.class)))
                .thenReturn(List.of(session(LocalDate.of(2026, 10, 20),
                        Discipline.CALISTHENICS, 30, hold)));

        StatsResponse stats = service.get(4);

        assertThat(stats.personalRecords()).singleElement().satisfies(pr -> {
            assertThat(pr.movement()).isEqualTo("Handstand Hold");
            assertThat(pr.bestSeconds()).isEqualByComparingTo("45");
            assertThat(pr.bestReps()).isNull();
        });
    }

    private static TrainingSession session(LocalDate date, Discipline discipline, int minutes,
                                           SessionEntry... entries) {
        return new TrainingSession(UUID.randomUUID(), ATHLETE_ID, date, discipline,
                SessionType.STRENGTH, "Sesión", null, minutes, null, null, null, null,
                OffsetDateTime.now(), List.of(entries));
    }

    private static SessionEntry entry(UUID exerciseId, String movement, int reps, String weight) {
        return new SessionEntry(UUID.randomUUID(), exerciseId, movement, 1, reps,
                new BigDecimal(weight), null, null);
    }
}
