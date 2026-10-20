package com.ruky.training.domain;

import java.util.UUID;

public record CompetitionDivision(
        UUID id,
        UUID competitionId,
        String name,
        AthleteTier tier,
        Integer capacity) {
}
