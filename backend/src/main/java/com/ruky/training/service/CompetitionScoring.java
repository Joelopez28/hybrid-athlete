package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.CompetitionDivision;
import com.ruky.training.domain.CompetitionEvent;
import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.CompetitionResult;
import com.ruky.training.domain.RegistrationStatus;
import com.ruky.training.domain.ScoreDirection;
import com.ruky.training.web.EventScoreResponse;
import com.ruky.training.web.LeaderboardResponse;
import com.ruky.training.web.LeaderboardRowResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Strong Fit scoring: per event, athletes are ranked by raw score and receive
 * (participants - place + 1) * 10 points. Missing results score zero. Total
 * points decide the division, with event wins as the first tie-breaker.
 */
@Component
public class CompetitionScoring {

    private static final int POINTS_PER_PLACE = 10;

    public List<LeaderboardResponse> score(
            List<CompetitionDivision> divisions,
            List<CompetitionEvent> events,
            List<CompetitionRegistration> registrations,
            List<CompetitionResult> results) {

        List<LeaderboardResponse> leaderboards = new ArrayList<>();
        for (CompetitionDivision division : divisions) {
            List<CompetitionRegistration> participants = registrations.stream()
                    .filter(registration -> registration.status() == RegistrationStatus.REGISTERED)
                    .filter(registration -> division.id().equals(registration.divisionId()))
                    .filter(registration -> registration.athlete() != null)
                    .toList();

            if (participants.isEmpty()) {
                leaderboards.add(new LeaderboardResponse(division.id(), division.name(), List.of()));
                continue;
            }

            Map<UUID, MutableRow> rows = new LinkedHashMap<>();
            for (CompetitionRegistration registration : participants) {
                rows.put(registration.athleteId(), new MutableRow(registration.athlete()));
            }

            for (CompetitionEvent event : events) {
                List<CompetitionResult> eventResults = results.stream()
                        .filter(result -> event.id().equals(result.eventId()))
                        .filter(result -> rows.containsKey(result.athleteId()))
                        .sorted(resultComparator(event.scoreDirection(), rows))
                        .toList();

                int participantsWithResult = eventResults.size();
                int place = 0;
                BigDecimal previousScore = null;

                for (int index = 0; index < eventResults.size(); index++) {
                    CompetitionResult result = eventResults.get(index);
                    if (previousScore == null || result.rawScore().compareTo(previousScore) != 0) {
                        place = index + 1;
                        previousScore = result.rawScore();
                    }
                    int points = Math.max(1, participantsWithResult - place + 1) * POINTS_PER_PLACE;

                    MutableRow row = rows.get(result.athleteId());
                    row.totalPoints += points;
                    if (place == 1) {
                        row.firstPlaces++;
                    }
                    row.scores.put(event.id(), new EventScoreResponse(
                            event.id(), place, points, result.displayScore()));
                }
            }

            List<MutableRow> ordered = rows.values().stream()
                    .sorted(Comparator
                            .comparingInt((MutableRow row) -> row.totalPoints).reversed()
                            .thenComparing(Comparator.comparingInt(
                                    (MutableRow row) -> row.firstPlaces).reversed())
                            .thenComparing(row -> row.athlete.displayName(),
                                    String.CASE_INSENSITIVE_ORDER))
                    .toList();

            List<LeaderboardRowResponse> board = new ArrayList<>();
            for (int index = 0; index < ordered.size(); index++) {
                MutableRow row = ordered.get(index);
                List<EventScoreResponse> scores = events.stream()
                        .map(event -> row.scores.getOrDefault(event.id(),
                                new EventScoreResponse(event.id(), null, 0, null)))
                        .toList();
                board.add(new LeaderboardRowResponse(
                        index + 1,
                        row.athlete.id(),
                        row.athlete.displayName(),
                        row.athlete.handle(),
                        row.athlete.tier(),
                        row.totalPoints,
                        row.firstPlaces,
                        scores));
            }
            leaderboards.add(new LeaderboardResponse(division.id(), division.name(), board));
        }
        return leaderboards;
    }

    private static Comparator<CompetitionResult> resultComparator(
            ScoreDirection direction, Map<UUID, MutableRow> rows) {
        Comparator<CompetitionResult> byScore = Comparator.comparing(CompetitionResult::rawScore);
        if (direction == ScoreDirection.DESC) {
            byScore = byScore.reversed();
        }
        return byScore.thenComparing(result -> rows.get(result.athleteId()).athlete.displayName(),
                String.CASE_INSENSITIVE_ORDER);
    }

    private static final class MutableRow {
        private final Athlete athlete;
        private final Map<UUID, EventScoreResponse> scores = new LinkedHashMap<>();
        private int totalPoints;
        private int firstPlaces;

        private MutableRow(Athlete athlete) {
            this.athlete = athlete;
        }
    }
}
