package com.ruky.training.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PersonalRecord(
        UUID exerciseId,
        String movement,
        BigDecimal bestWeightKg,
        Integer bestReps,
        BigDecimal bestSeconds,
        LocalDate achievedOn) {
}
