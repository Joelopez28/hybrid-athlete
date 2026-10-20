package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.EliteStandard;
import com.ruky.training.domain.SessionEntry;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.postgrest.EliteRepository;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.web.AthleteResponse;
import com.ruky.training.web.EliteCriterionResponse;
import com.ruky.training.web.EliteStatusResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EliteService {

    private static final int REQUIRED_STANDARDS = 3;

    private final EliteRepository repository;
    private final SessionRepository sessions;
    private final CurrentAthleteService currentAthlete;
    private final Clock clock;

    public EliteService(EliteRepository repository,
                        SessionRepository sessions,
                        CurrentAthleteService currentAthlete,
                        Clock clock) {
        this.repository = repository;
        this.sessions = sessions;
        this.currentAthlete = currentAthlete;
        this.clock = clock;
    }

    public EliteStatusResponse status() {
        Athlete athlete = currentAthlete.get();
        List<EliteStandard> standards = repository.findStandards();
        List<TrainingSession> history = sessions.findAllSince(athlete.id(),
                LocalDate.now(clock).minusDays(365));
        return evaluate(athlete, standards, history);
    }

    public EliteStatusResponse apply() {
        EliteStatusResponse current = status();
        if (current.tier() == AthleteTier.ELITE) {
            return current;
        }
        if (!current.qualified()) {
            throw new IllegalArgumentException(
                    "Todavía no cumplís los estándares para Ruky Elite: "
                            + current.metCount() + "/" + current.requiredCount());
        }
        if (!repository.promote(current.athlete().id())) {
            throw new IllegalStateException("No se pudo actualizar la división");
        }
        return status();
    }

    private EliteStatusResponse evaluate(Athlete athlete, List<EliteStandard> standards,
                                         List<TrainingSession> history) {
        List<EliteCriterionResponse> criteria = new ArrayList<>();
        for (EliteStandard standard : standards) {
            criteria.add(switch (standard.metric()) {
                case "TOTAL_RATIO" -> totalRatio(athlete, standard, history);
                case "FRAN_TIME" -> franTime(standard, history);
                case "HYROX_TIME" -> hyroxTime(standard, history);
                case "PULLUP_RATIO" -> pullUpRatio(athlete, standard, history);
                default -> new EliteCriterionResponse(standard.code(), standard.discipline(),
                        standard.name(), standard.target(), BigDecimal.ZERO, standard.unit(), false,
                        BigDecimal.ZERO, "Estándar no reconocido");
            });
        }
        int met = (int) criteria.stream().filter(EliteCriterionResponse::met).count();
        int required = Math.min(REQUIRED_STANDARDS, criteria.size());
        boolean qualified = met >= required;
        String message;
        if (athlete.tier() == AthleteTier.ELITE) {
            message = "Ya formás parte de Ruky Elite.";
        } else if (qualified) {
            message = "Cumplís los estándares. Podés solicitar tu clasificación.";
        } else {
            message = "Te faltan " + (required - met) + " estándares para clasificar.";
        }
        return new EliteStatusResponse(AthleteResponse.from(athlete), athlete.tier(), qualified,
                met, required, criteria, message);
    }

    private static EliteCriterionResponse totalRatio(Athlete athlete, EliteStandard standard,
                                                      List<TrainingSession> history) {
        BigDecimal squat = bestWeight(history, "Back Squat");
        BigDecimal bench = bestWeight(history, "Bench Press");
        BigDecimal deadlift = bestWeight(history, "Deadlift");
        BigDecimal total = squat.add(bench).add(deadlift);
        BigDecimal ratio = athlete.bodyweightKg() == null || athlete.bodyweightKg().signum() == 0
                ? BigDecimal.ZERO
                : total.divide(athlete.bodyweightKg(), 2, RoundingMode.HALF_UP);
        boolean met = ratio.compareTo(standard.target()) >= 0;
        String detail = athlete.bodyweightKg() == null
                ? "Cargá tu peso corporal para calcular el ratio"
                : total.setScale(1, RoundingMode.HALF_UP) + " kg / "
                        + athlete.bodyweightKg().stripTrailingZeros().toPlainString() + " kg";
        return criterion(standard, ratio, met, detail);
    }

    private static EliteCriterionResponse franTime(EliteStandard standard,
                                                    List<TrainingSession> history) {
        Integer seconds = history.stream()
                .filter(session -> session.sessionType() == SessionType.WOD)
                .filter(session -> session.title() != null
                        && session.title().toLowerCase().contains("fran"))
                .map(TrainingSession::wodScore)
                .map(EliteService::parseSeconds)
                .filter(value -> value != null)
                .min(Integer::compareTo)
                .orElse(0);
        return timeCriterion(standard, seconds);
    }

    private static EliteCriterionResponse hyroxTime(EliteStandard standard,
                                                     List<TrainingSession> history) {
        Integer seconds = history.stream()
                .filter(session -> session.discipline() == Discipline.HYROX)
                .filter(session -> session.sessionType() == SessionType.HYROX_SIM)
                .map(TrainingSession::wodScore)
                .map(EliteService::parseSeconds)
                .filter(value -> value != null)
                .min(Integer::compareTo)
                .orElse(0);
        return timeCriterion(standard, seconds);
    }

    private static EliteCriterionResponse pullUpRatio(Athlete athlete, EliteStandard standard,
                                                       List<TrainingSession> history) {
        BigDecimal weight = bestWeight(history, "Weighted Pull-up");
        BigDecimal ratio = athlete.bodyweightKg() == null || athlete.bodyweightKg().signum() == 0
                ? BigDecimal.ZERO
                : weight.divide(athlete.bodyweightKg(), 2, RoundingMode.HALF_UP);
        boolean met = ratio.compareTo(standard.target()) >= 0;
        String detail = athlete.bodyweightKg() == null
                ? "Cargá tu peso corporal para calcular el ratio"
                : weight.setScale(1, RoundingMode.HALF_UP) + " kg / "
                        + athlete.bodyweightKg().stripTrailingZeros().toPlainString() + " kg";
        return criterion(standard, ratio, met, detail);
    }

    private static EliteCriterionResponse timeCriterion(EliteStandard standard, int seconds) {
        boolean met = seconds > 0 && seconds <= standard.target().intValue();
        BigDecimal progress = seconds <= 0
                ? BigDecimal.ZERO
                : standard.target().divide(BigDecimal.valueOf(seconds), 2, RoundingMode.HALF_UP)
                        .min(BigDecimal.ONE);
        String detail = seconds <= 0
                ? "Todavía no registraste esta prueba"
                : formatSeconds(seconds) + " / objetivo " + formatSeconds(standard.target().intValue());
        return new EliteCriterionResponse(standard.code(), standard.discipline(), standard.name(),
                standard.target(), BigDecimal.valueOf(seconds), standard.unit(), met, progress, detail);
    }

    private static EliteCriterionResponse criterion(EliteStandard standard, BigDecimal actual,
                                                     boolean met, String detail) {
        BigDecimal progress = standard.target().signum() == 0
                ? BigDecimal.ZERO
                : actual.divide(standard.target(), 2, RoundingMode.HALF_UP).min(BigDecimal.ONE);
        return new EliteCriterionResponse(standard.code(), standard.discipline(), standard.name(),
                standard.target(), actual, standard.unit(), met, progress, detail);
    }

    private static BigDecimal bestWeight(List<TrainingSession> history, String movement) {
        return history.stream()
                .filter(session -> session.sessionType() == SessionType.STRENGTH
                        || session.sessionType() == SessionType.SKILL)
                .flatMap(session -> session.entries().stream())
                .filter(entry -> entry.movement() != null
                        && entry.movement().equalsIgnoreCase(movement))
                .map(SessionEntry::weightKg)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static Integer parseSeconds(String value) {
        if (value == null || value.isBlank() || value.contains("+")) {
            return null;
        }
        String[] parts = value.trim().split(":");
        try {
            if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600
                        + Integer.parseInt(parts[1]) * 60
                        + Integer.parseInt(parts[2]);
            }
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatSeconds(int total) {
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }
}
