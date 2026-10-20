package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Exercise;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.Sex;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.domain.WodFormat;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.postgrest.ExerciseRepository;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.postgrest.SessionWrite;
import com.ruky.training.web.CreateSessionRequest;
import com.ruky.training.web.EntryRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final UUID ATHLETE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EXERCISE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SESSION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private SessionRepository sessions;

    @Mock
    private ExerciseRepository exercises;

    @Mock
    private CurrentAthleteService currentAthlete;

    @InjectMocks
    private SessionService service;

    private final Athlete athlete = new Athlete(ATHLETE_ID, "ruky", "Ruky", Sex.MALE, null,
            new BigDecimal("82.0"), "CL", null, AthleteTier.ELITE,
            List.of(Discipline.POWERLIFTING));

    private final Exercise squat = new Exercise(EXERCISE_ID, "back-squat", "Back Squat",
            Discipline.POWERLIFTING, "BARBELL", "KG", true);

    @BeforeEach
    void setUp() {
        when(currentAthlete.get()).thenReturn(athlete);
    }

    @Test
    void shouldCreateStrengthSessionAndDenormalizeExerciseName() {
        when(exercises.findById(EXERCISE_ID)).thenReturn(Optional.of(squat));
        when(sessions.create(any(SessionWrite.class))).thenAnswer(invocation -> {
            SessionWrite write = invocation.getArgument(0);
            List<SessionEntry> entries = write.entries().stream()
                    .map(entry -> new SessionEntry(UUID.randomUUID(), entry.exerciseId(),
                            entry.movement(), entry.setIndex(), entry.reps(), entry.weightKg(),
                            entry.seconds(), entry.rpe()))
                    .toList();
            return new TrainingSession(SESSION_ID, write.athleteId(), write.performedOn(),
                    write.discipline(), write.sessionType(), write.title(), write.notes(),
                    write.durationMin(), write.rpe(), write.wodFormat(), write.wodScore(),
                    write.wodDivision(), OffsetDateTime.now(), entries);
        });

        TrainingSession created = service.create(new CreateSessionRequest(
                LocalDate.of(2026, 10, 20), Discipline.POWERLIFTING, SessionType.STRENGTH,
                "  Fuerza  ", "  ", 60, new BigDecimal("8.0"), null, null, null,
                List.of(new EntryRequest(EXERCISE_ID, "ignored", null, 5, new BigDecimal("100.0"), null, null))));

        ArgumentCaptor<SessionWrite> captor = ArgumentCaptor.forClass(SessionWrite.class);
        verify(sessions).create(captor.capture());
        SessionWrite write = captor.getValue();
        assertThat(write.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(write.title()).isEqualTo("Fuerza");
        assertThat(write.notes()).isNull();
        assertThat(write.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.movement()).isEqualTo("Back Squat");
            assertThat(entry.setIndex()).isEqualTo(1);
        });
        assertThat(created.title()).isEqualTo("Fuerza");
    }

    @Test
    void shouldRejectStrengthSessionWithoutEntries() {
        assertThatThrownBy(() -> service.create(new CreateSessionRequest(
                LocalDate.of(2026, 10, 20), Discipline.POWERLIFTING, SessionType.STRENGTH,
                "Fuerza", null, 60, null, null, null, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serie");
    }

    @Test
    void shouldRejectWodWithoutFormatOrScore() {
        assertThatThrownBy(() -> service.create(new CreateSessionRequest(
                LocalDate.of(2026, 10, 20), Discipline.CROSSFIT, SessionType.WOD,
                "Fran", null, 12, null, null, null, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formato");
    }

    @Test
    void shouldRequireScoreForHyroxSimulation() {
        assertThatThrownBy(() -> service.create(new CreateSessionRequest(
                LocalDate.of(2026, 10, 20), Discipline.HYROX, SessionType.HYROX_SIM,
                "HYROX", null, 80, null, null, null, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tiempo");
    }

    @Test
    void shouldRejectEntryWithoutRepsOrSeconds() {
        assertThatThrownBy(() -> service.create(new CreateSessionRequest(
                LocalDate.of(2026, 10, 20), Discipline.POWERLIFTING, SessionType.STRENGTH,
                "Fuerza", null, 60, null, null, null, null,
                List.of(new EntryRequest(null, "Custom", 1, null, null, null, null)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repeticiones");
    }

    @Test
    void shouldHideSessionsFromOtherAthletes() {
        TrainingSession foreign = new TrainingSession(SESSION_ID, OTHER_ID, LocalDate.now(),
                Discipline.CROSSFIT, SessionType.WOD, "Ajena", null, null, null, WodFormat.FOR_TIME,
                "05:00", null, OffsetDateTime.now(), List.of());
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.get(SESSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
