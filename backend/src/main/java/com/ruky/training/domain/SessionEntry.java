package com.ruky.training.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record SessionEntry(
        UUID id,
        UUID exerciseId,
        String movement,
        Integer setIndex,
        Integer reps,
        BigDecimal weightKg,
        BigDecimal seconds,
        BigDecimal rpe) {

    public BigDecimal volumeKg() {
        if (reps == null || weightKg == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(reps).multiply(weightKg);
    }
}
