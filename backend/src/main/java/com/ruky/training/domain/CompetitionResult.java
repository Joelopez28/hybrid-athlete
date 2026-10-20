package com.ruky.training.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record CompetitionResult(
        UUID id,
        UUID competitionId,
        UUID eventId,
        UUID athleteId,
        BigDecimal rawScore,
        String displayScore,
        String notes) {
}
