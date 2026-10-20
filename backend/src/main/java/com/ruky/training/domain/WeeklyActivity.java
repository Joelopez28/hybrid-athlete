package com.ruky.training.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WeeklyActivity(
        UUID athleteId,
        LocalDate weekStart,
        int sessions,
        int minutes,
        BigDecimal volumeKg) {
}
