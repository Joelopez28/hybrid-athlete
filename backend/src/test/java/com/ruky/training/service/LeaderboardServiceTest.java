package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.Sex;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.postgrest.AthleteRepository;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.web.LeaderboardEntryResponse;
import com.ruky.training.web.LeaderboardMetric;
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
class LeaderboardServiceTest {

    private static final UUID ME = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VALE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SOFI = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private SessionRepository sessions;

    @Mock
    private AthleteRepository athletes;

    @Mock
    private CurrentAthleteService currentAthlete;

    private LeaderboardService service;

    private final Athlete me = athlete(ME, "ruky", "Ruky", AthleteTier.ELITE);
    private final Athlete vale = athlete(VALE, "vale", "Vale", AthleteTier.ELITE);
    private final Athlete sofi = athlete(SOFI, "sofi", "Sofi", AthleteTier.OPEN);

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(sessions, athletes, currentAthlete,
                Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));
        when(currentAthlete.get()).thenReturn(me);
        when(athletes.findAll()).thenReturn(List.of(me, vale, sofi));
    }

    @Test
    void shouldRankByVolumeAndIncludeOnlyAthletesWithSessions() {
        when(sessions.findAllSince(any(LocalDate.class))).thenReturn(List.of(
                session(ME, "600"), session(ME, "400"),
                session(VALE, "2000")));

        List<LeaderboardEntryResponse> board = service.get(4, LeaderboardMetric.VOLUME, null);

        assertThat(board).hasSize(2);
        assertThat(board.get(0).handle()).isEqualTo("vale");
        assertThat(board.get(0).value()).isEqualByComparingTo("2000");
        assertThat(board.get(0).rank()).isEqualTo(1);
        assertThat(board.get(1).handle()).isEqualTo("ruky");
        assertThat(board.get(1).me()).isTrue();
        assertThat(board.get(1).sessions()).isEqualTo(2);
    }

    @Test
    void shouldFilterEliteDivision() {
        when(sessions.findAllSince(any(LocalDate.class))).thenReturn(List.of(
                session(ME, "600"), session(VALE, "2000"), session(SOFI, "3000")));

        List<LeaderboardEntryResponse> elite = service.get(4, LeaderboardMetric.VOLUME, AthleteTier.ELITE);

        assertThat(elite).extracting(LeaderboardEntryResponse::handle).containsExactly("vale", "ruky");
        assertThat(elite).allMatch(entry -> entry.tier() == AthleteTier.ELITE);
    }

    @Test
    void shouldRankBySessionCount() {
        when(sessions.findAllSince(any(LocalDate.class))).thenReturn(List.of(
                session(ME, "100"), session(ME, "100"),
                session(VALE, "500")));

        List<LeaderboardEntryResponse> board = service.get(4, LeaderboardMetric.SESSIONS, null);

        assertThat(board.get(0).handle()).isEqualTo("ruky");
        assertThat(board.get(0).value()).isEqualByComparingTo("2");
        assertThat(board.get(1).value()).isEqualByComparingTo("1");
    }

    private static Athlete athlete(UUID id, String handle, String name, AthleteTier tier) {
        return new Athlete(id, handle, name, Sex.MALE, null, new BigDecimal("80"), "CL", null,
                tier, List.of(Discipline.POWERLIFTING));
    }

    private static TrainingSession session(UUID athleteId, String weight) {
        SessionEntry entry = new SessionEntry(UUID.randomUUID(), UUID.randomUUID(), "Back Squat",
                1, 1, new BigDecimal(weight), null, null);
        return new TrainingSession(UUID.randomUUID(), athleteId, LocalDate.of(2026, 10, 20),
                Discipline.POWERLIFTING, SessionType.STRENGTH, "Sesión", null, 60, null,
                null, null, null, OffsetDateTime.now(), List.of(entry));
    }
}
