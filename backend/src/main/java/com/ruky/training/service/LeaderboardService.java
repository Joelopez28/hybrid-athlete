package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.WeeklyActivity;
import com.ruky.training.postgrest.ActivityRepository;
import com.ruky.training.postgrest.AthleteRepository;
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

    private final ActivityRepository activity;
    private final AthleteRepository athletes;
    private final CurrentAthleteService currentAthlete;
    private final Clock clock;

    public LeaderboardService(ActivityRepository activity,
                              AthleteRepository athletes,
                              CurrentAthleteService currentAthlete,
                              Clock clock) {
        this.activity = activity;
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

        Map<UUID, List<WeeklyActivity>> byAthlete = new HashMap<>();
        for (WeeklyActivity row : activity.findSince(start)) {
            byAthlete.computeIfAbsent(row.athleteId(), key -> new ArrayList<>()).add(row);
        }

        Athlete me = currentAthlete.get();
        List<Row> rows = new ArrayList<>();
        for (Athlete athlete : athletes.findAll()) {
            if (tier != null && athlete.tier() != tier) {
                continue;
            }
            List<WeeklyActivity> athleteActivity = byAthlete.get(athlete.id());
            if (athleteActivity == null || athleteActivity.isEmpty()) {
                continue;
            }
            rows.add(new Row(athlete, value(athleteActivity, effectiveMetric),
                    athleteActivity.stream().mapToInt(WeeklyActivity::sessions).sum()));
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

    private static BigDecimal value(List<WeeklyActivity> activity, LeaderboardMetric metric) {
        return switch (metric) {
            case VOLUME -> activity.stream()
                    .map(WeeklyActivity::volumeKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(1, RoundingMode.HALF_UP);
            case SESSIONS -> BigDecimal.valueOf(activity.stream()
                    .mapToInt(WeeklyActivity::sessions).sum());
            case MINUTES -> BigDecimal.valueOf(activity.stream()
                    .mapToInt(WeeklyActivity::minutes).sum());
        };
    }

    private record Row(Athlete athlete, BigDecimal value, int sessions) {
    }
}
