package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.postgrest.AthleteRepository;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.web.LeaderboardEntryResponse;
import com.ruky.training.web.LeaderboardMetric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private static final int LIMIT = 20;

    private final SessionRepository sessions;
    private final AthleteRepository athletes;
    private final CurrentAthleteService currentAthlete;
    private final Clock clock;

    public LeaderboardService(SessionRepository sessions,
                              AthleteRepository athletes,
                              CurrentAthleteService currentAthlete,
                              Clock clock) {
        this.sessions = sessions;
        this.athletes = athletes;
        this.currentAthlete = currentAthlete;
        this.clock = clock;
    }

    public List<LeaderboardEntryResponse> get(int weeks, LeaderboardMetric metric, AthleteTier tier) {
        if (weeks < 1 || weeks > 52) {
            throw new IllegalArgumentException("weeks debe estar entre 1 y 52");
        }
        LeaderboardMetric effectiveMetric = metric == null ? LeaderboardMetric.VOLUME : metric;
        LocalDate start = LocalDate.now(clock)
                .with(DayOfWeek.MONDAY)
                .minusWeeks(weeks - 1L);

        Map<UUID, List<TrainingSession>> byAthlete = new HashMap<>();
        for (TrainingSession session : sessions.findAllSince(start)) {
            byAthlete.computeIfAbsent(session.athleteId(), key -> new ArrayList<>()).add(session);
        }

        Athlete me = currentAthlete.get();
        List<Row> rows = new ArrayList<>();
        for (Athlete athlete : athletes.findAll()) {
            if (tier != null && athlete.tier() != tier) {
                continue;
            }
            List<TrainingSession> athleteSessions = byAthlete.get(athlete.id());
            if (athleteSessions == null || athleteSessions.isEmpty()) {
                continue;
            }
            rows.add(new Row(athlete, value(athleteSessions, effectiveMetric), athleteSessions.size()));
        }

        rows.sort(Comparator.comparing(Row::value).reversed()
                .thenComparing(Row::sessions, Comparator.reverseOrder())
                .thenComparing(row -> row.athlete().displayName(), String.CASE_INSENSITIVE_ORDER));

        List<LeaderboardEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (Row row : rows) {
            if (rank > LIMIT) {
                break;
            }
            result.add(new LeaderboardEntryResponse(
                    rank++,
                    row.athlete().id(),
                    row.athlete().displayName(),
                    row.athlete().handle(),
                    row.athlete().tier(),
                    row.value(),
                    row.sessions(),
                    row.athlete().id().equals(me.id())));
        }
        return result;
    }

    private static BigDecimal value(List<TrainingSession> sessions, LeaderboardMetric metric) {
        return switch (metric) {
            case VOLUME -> sessions.stream()
                    .map(TrainingSession::volumeKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(1, RoundingMode.HALF_UP);
            case SESSIONS -> BigDecimal.valueOf(sessions.size());
            case MINUTES -> BigDecimal.valueOf(sessions.stream()
                    .mapToInt(session -> session.durationMin() == null ? 0 : session.durationMin())
                    .sum());
        };
    }

    private record Row(Athlete athlete, BigDecimal value, int sessions) {
    }
}
