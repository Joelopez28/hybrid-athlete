package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.CompetitionDivision;
import com.ruky.training.domain.CompetitionEvent;
import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.CompetitionResult;
import com.ruky.training.domain.RegistrationStatus;
import com.ruky.training.domain.ScoreDirection;
import com.ruky.training.domain.ScoreType;
import com.ruky.training.domain.Sex;
import com.ruky.training.web.LeaderboardResponse;
import com.ruky.training.web.LeaderboardRowResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompetitionScoringTest {

    private static final UUID COMPETITION_ID = UUID.randomUUID();
    private static final UUID DIVISION_ID = UUID.randomUUID();
    private static final UUID TIME_EVENT = UUID.randomUUID();
    private static final UUID REPS_EVENT = UUID.randomUUID();

    private final CompetitionScoring scoring = new CompetitionScoring();

    private final Athlete ruky = athlete("Ruky", "ruky");
    private final Athlete vale = athlete("Vale", "vale");
    private final Athlete nacho = athlete("Nacho", "nacho");
    private final Athlete sofi = athlete("Sofi", "sofi");

    @Test
    void shouldRankPerEventAndSumPointsWithMissingResultsAsZero() {
        CompetitionDivision division = new CompetitionDivision(DIVISION_ID, COMPETITION_ID, "RX",
                AthleteTier.OPEN, 10);
        List<CompetitionEvent> events = List.of(
                new CompetitionEvent(TIME_EVENT, COMPETITION_ID, "Fran", null, ScoreType.TIME,
                        ScoreDirection.ASC, 1),
                new CompetitionEvent(REPS_EVENT, COMPETITION_ID, "Pull-ups", null, ScoreType.REPS,
                        ScoreDirection.DESC, 2));

        List<CompetitionRegistration> registrations = List.of(
                registration(ruky, division), registration(vale, division),
                registration(nacho, division), registration(sofi, division));

        List<CompetitionResult> results = List.of(
                result(TIME_EVENT, ruky, "260", "04:20"),
                result(TIME_EVENT, vale, "300", "05:00"),
                result(TIME_EVENT, nacho, "330", "05:30"),
                result(REPS_EVENT, ruky, "20", "20 reps"),
                result(REPS_EVENT, vale, "25", "25 reps"),
                result(REPS_EVENT, sofi, "15", "15 reps"));

        LeaderboardResponse board = scoring.score(List.of(division), events, registrations, results).get(0);

        assertThat(board.rows()).extracting(LeaderboardRowResponse::handle)
                .containsExactly("ruky", "vale", "nacho", "sofi");

        LeaderboardRowResponse first = board.rows().get(0);
        assertThat(first.totalPoints()).isEqualTo(50); // 30 + 20
        assertThat(first.firstPlaces()).isEqualTo(1);
        assertThat(first.eventScores()).extracting(score -> score.points()).containsExactly(30, 20);

        LeaderboardRowResponse third = board.rows().get(2);
        assertThat(third.totalPoints()).isEqualTo(10);
        assertThat(third.eventScores().get(1).points()).isZero();
        assertThat(third.eventScores().get(1).place()).isNull();

        LeaderboardRowResponse fourth = board.rows().get(3);
        assertThat(fourth.totalPoints()).isEqualTo(10); // only pull-ups
    }

    @Test
    void shouldSharePlaceAndPointsOnTies() {
        CompetitionDivision division = new CompetitionDivision(DIVISION_ID, COMPETITION_ID, "RX",
                AthleteTier.OPEN, 10);
        List<CompetitionEvent> events = List.of(
                new CompetitionEvent(TIME_EVENT, COMPETITION_ID, "Fran", null, ScoreType.TIME,
                        ScoreDirection.ASC, 1));
        List<CompetitionRegistration> registrations = List.of(
                registration(ruky, division), registration(vale, division));
        List<CompetitionResult> results = List.of(
                result(TIME_EVENT, ruky, "260", "04:20"),
                result(TIME_EVENT, vale, "260", "04:20"));

        LeaderboardResponse board = scoring.score(List.of(division), events, registrations, results).get(0);

        assertThat(board.rows()).allSatisfy(row -> {
            assertThat(row.totalPoints()).isEqualTo(20);
            assertThat(row.eventScores().get(0).place()).isEqualTo(1);
        });
        assertThat(board.rows()).extracting(LeaderboardRowResponse::handle).containsExactly("ruky", "vale");
    }

    private static Athlete athlete(String name, String handle) {
        return new Athlete(UUID.randomUUID(), handle, name, Sex.MALE, null,
                new BigDecimal("80"), "CL", null, AthleteTier.OPEN, List.of());
    }

    private static CompetitionRegistration registration(Athlete athlete, CompetitionDivision division) {
        return new CompetitionRegistration(UUID.randomUUID(), COMPETITION_ID, division.id(),
                athlete.id(), RegistrationStatus.REGISTERED, athlete);
    }

    private static CompetitionResult result(UUID eventId, Athlete athlete, String raw, String display) {
        return new CompetitionResult(UUID.randomUUID(), COMPETITION_ID, eventId, athlete.id(),
                new BigDecimal(raw), display, null);
    }
}
