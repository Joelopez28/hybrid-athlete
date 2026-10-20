package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Sex;
import com.ruky.training.domain.WeeklyActivity;
import com.ruky.training.postgrest.ActivityRepository;
import com.ruky.training.postgrest.AthleteRepository;
import com.ruky.training.web.LeaderboardEntryResponse;
import com.ruky.training.web.LeaderboardMetric;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);

    @Mock
    private ActivityRepository activity;

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
        service = new LeaderboardService(activity, athletes, currentAthlete,
                Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));
        lenient().when(currentAthlete.get()).thenReturn(me);
        lenient().when(athletes.findAll()).thenReturn(List.of(me, vale, sofi));
    }

    @Test
    void shouldRankByAggregatedVolumeAndIncludeOnlyActiveAthletes() {
        when(activity.findSince(any(LocalDate.class))).thenReturn(List.of(
                weekly(ME, 2, 120, "600"), weekly(ME, 1, 60, "400"),
                weekly(VALE, 4, 240, "2000")));

        List<LeaderboardEntryResponse> board = service.get(4, LeaderboardMetric.VOLUME, null);

        assertThat(board).hasSize(2);
        assertThat(board.get(0).handle()).isEqualTo("vale");
        assertThat(board.get(0).value()).isEqualByComparingTo("2000");
        assertThat(board.get(0).sessions()).isEqualTo(4);
        assertThat(board.get(1).handle()).isEqualTo("ruky");
        assertThat(board.get(1).value()).isEqualByComparingTo("1000");
        assertThat(board.get(1).sessions()).isEqualTo(3);
        assertThat(board.get(1).me()).isTrue();
    }

    @Test
    void shouldFilterEliteDivision() {
        when(activity.findSince(any(LocalDate.class))).thenReturn(List.of(
                weekly(ME, 1, 60, "600"), weekly(VALE, 1, 60, "2000"),
                weekly(SOFI, 1, 60, "3000")));

        List<LeaderboardEntryResponse> elite = service.get(4, LeaderboardMetric.VOLUME, AthleteTier.ELITE);

        assertThat(elite).extracting(LeaderboardEntryResponse::handle).containsExactly("vale", "ruky");
        assertThat(elite).allMatch(entry -> entry.tier() == AthleteTier.ELITE);
    }

    @Test
    void shouldRankByAggregatedSessions() {
        when(activity.findSince(any(LocalDate.class))).thenReturn(List.of(
                weekly(ME, 2, 120, "100"), weekly(VALE, 5, 240, "500")));

        List<LeaderboardEntryResponse> board = service.get(4, LeaderboardMetric.SESSIONS, null);

        assertThat(board.get(0).handle()).isEqualTo("vale");
        assertThat(board.get(0).value()).isEqualByComparingTo("5");
        assertThat(board.get(1).value()).isEqualByComparingTo("2");
    }

    private static Athlete athlete(UUID id, String handle, String name, AthleteTier tier) {
        return new Athlete(id, handle, name, Sex.MALE, null, new BigDecimal("80"), "CL", null,
                tier, List.of(Discipline.POWERLIFTING));
    }

    private static WeeklyActivity weekly(UUID athleteId, int sessions, int minutes, String volume) {
        return new WeeklyActivity(athleteId, WEEK, sessions, minutes, new BigDecimal(volume));
    }
}
