package com.ruky.training.postgrest;

import java.math.BigDecimal;
import java.util.UUID;

public record EntryWrite(
        UUID exerciseId,
        String movement,
        Integer setIndex,
        Integer reps,
        BigDecimal weightKg,
        BigDecimal seconds,
        BigDecimal rpe) {
}
