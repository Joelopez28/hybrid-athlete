package com.ruky.training.postgrest;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultWrite(
        UUID competitionId,
        UUID eventId,
        UUID athleteId,
        BigDecimal rawScore,
        String displayScore,
        String notes) {
}
