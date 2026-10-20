package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.web.AthleteResponse;
import com.ruky.training.web.DisciplineBreakdown;
import com.ruky.training.web.PersonalRecord;
import com.ruky.training.web.StatsResponse;
import com.ruky.training.web.Totals;
import com.ruky.training.web.WeeklyPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

    private static final int MAX_PR_ROWS = 10;

    private final SessionRepository sessions;
    private final CurrentAthleteService currentAthlete;
    private final Clock clock;

    public StatsService(SessionRepository sessions,
                        CurrentAthleteService currentAthlete,
                        Clock clock) {
        this.sessions = sessions;
        this.currentAthlete = currentAthlete;
        this.clock = clock;
    }

    public StatsResponse get(int weeks) {
        if (weeks < 1 || weeks > 52) {
            throw new IllegalArgumentException("weeks debe estar entre 1 y 52");
        }
        Athlete athlete = currentAthlete.get();
        LocalDate currentWeek = LocalDate.now(clock).with(DayOfWeek.MONDAY);
        LocalDate start = currentWeek.minusWeeks(weeks - 1L);

        List<TrainingSession> history = sessions.findAllSince(athlete.id(), start);
        Map<LocalDate, WeekAccumulator> weekly = new LinkedHashMap<>();
        for (int i = 0; i < weeks; i++) {
            weekly.put(start.plusWeeks(i), new WeekAccumulator());
        }

        int totalSessions = 0;
        int totalMinutes = 0;
        BigDecimal totalVolume = BigDecimal.ZERO;
        Map<String, PrCandidate> prs = new LinkedHashMap<>();
        Map<Discipline, DisciplineAccumulator> byDiscipline = new LinkedHashMap<>();

        for (TrainingSession session : history) {
            totalSessions++;
            int minutes = session.durationMin() == null ? 0 : session.durationMin();
            totalMinutes += minutes;
            BigDecimal volume = session.volumeKg();
            totalVolume = totalVolume.add(volume);

            WeekAccumulator week = weekly.get(session.performedOn().with(DayOfWeek.MONDAY));
            if (week != null) {
                week.sessions++;
                week.minutes += minutes;
                week.volume = week.volume.add(volume);
            }

            byDiscipline.computeIfAbsent(session.discipline(), key -> new DisciplineAccumulator())
                    .add(volume);

            // WOD/HYROX entries are aggregate results, not set-level PRs.
            boolean recordPrs = session.sessionType() == SessionType.STRENGTH
                    || session.sessionType() == SessionType.SKILL;
            if (!recordPrs) {
                continue;
            }

            for (SessionEntry entry : session.entries()) {
                if (entry.reps() == null && entry.seconds() == null && entry.weightKg() == null) {
                    continue;
                }
                String key = entry.exerciseId() != null
                        ? "e:" + entry.exerciseId()
                        : "m:" + entry.movement().toLowerCase(Locale.ROOT);
                PrCandidate candidate = prs.get(key);
                if (isBetter(candidate, entry.weightKg(), entry.reps(), entry.seconds())) {
                    prs.put(key, new PrCandidate(entry.exerciseId(), entry.movement(),
                            entry.weightKg(), entry.reps(), entry.seconds(), session.performedOn()));
                }
            }
        }

        List<WeeklyPoint> weeklyPoints = weekly.entrySet().stream()
                .map(entry -> new WeeklyPoint(
                        entry.getKey(),
                        entry.getValue().sessions,
                        entry.getValue().minutes,
                        entry.getValue().volume.setScale(1, RoundingMode.HALF_UP)))
                .toList();

        List<PersonalRecord> records = prs.values().stream()
                .map(pr -> new PersonalRecord(pr.exerciseId(), pr.movement(), pr.bestWeightKg(),
                        pr.bestReps(), pr.bestSeconds(), pr.achievedOn()))
                .sorted(Comparator
                        .comparing(PersonalRecord::bestWeightKg,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PersonalRecord::bestReps,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PersonalRecord::bestSeconds,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_PR_ROWS)
                .toList();

        List<DisciplineBreakdown> breakdown = byDiscipline.entrySet().stream()
                .map(entry -> new DisciplineBreakdown(
                        entry.getKey(),
                        entry.getValue().sessions,
                        entry.getValue().volume.setScale(1, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(DisciplineBreakdown::volumeKg).reversed())
                .toList();

        Totals totals = new Totals(totalSessions, totalMinutes,
                totalVolume.setScale(1, RoundingMode.HALF_UP), prs.size());

        return new StatsResponse(AthleteResponse.from(athlete), totals, weeklyPoints, records, breakdown);
    }

    private static boolean isBetter(PrCandidate current, BigDecimal weight, Integer reps,
                                    BigDecimal seconds) {
        if (current == null) {
            return true;
        }
        int weightComparison = compareNullable(weight, current.bestWeightKg());
        if (weightComparison != 0) {
            return weightComparison > 0;
        }
        if (reps != null || current.bestReps() != null) {
            int repsComparison = Integer.compare(reps == null ? 0 : reps,
                    current.bestReps() == null ? 0 : current.bestReps());
            if (repsComparison != 0) {
                return repsComparison > 0;
            }
        }
        return seconds != null
                && (current.bestSeconds() == null || seconds.compareTo(current.bestSeconds()) > 0);
    }

    private static int compareNullable(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static final class WeekAccumulator {
        private int sessions;
        private int minutes;
        private BigDecimal volume = BigDecimal.ZERO;
    }

    private static final class DisciplineAccumulator {
        private int sessions;
        private BigDecimal volume = BigDecimal.ZERO;

        void add(BigDecimal sessionVolume) {
            sessions++;
            volume = volume.add(sessionVolume);
        }
    }

    private record PrCandidate(
            java.util.UUID exerciseId,
            String movement,
            BigDecimal bestWeightKg,
            Integer bestReps,
            BigDecimal bestSeconds,
            LocalDate achievedOn) {
    }
}
