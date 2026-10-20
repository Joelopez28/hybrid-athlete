package com.ruky.training.domain;

import java.util.UUID;

public record CompetitionEvent(
        UUID id,
        UUID competitionId,
        String name,
        String description,
        ScoreType scoreType,
        ScoreDirection scoreDirection,
        int orderIndex) {
}
