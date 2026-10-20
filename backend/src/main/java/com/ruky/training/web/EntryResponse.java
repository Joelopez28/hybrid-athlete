package com.ruky.training.web;

import com.ruky.training.domain.SessionEntry;
import java.math.BigDecimal;
import java.util.UUID;

public record EntryResponse(
        UUID id,
        UUID exerciseId,
        String movement,
        Integer setIndex,
        Integer reps,
        BigDecimal weightKg,
        BigDecimal seconds,
        BigDecimal rpe) {

    public static EntryResponse from(SessionEntry entry) {
        return new EntryResponse(entry.id(), entry.exerciseId(), entry.movement(), entry.setIndex(),
                entry.reps(), entry.weightKg(), entry.seconds(), entry.rpe());
    }
}
