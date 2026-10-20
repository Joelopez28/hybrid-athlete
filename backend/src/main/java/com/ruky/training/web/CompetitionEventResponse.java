package com.ruky.training.web;

import com.ruky.training.domain.CompetitionEvent;
import com.ruky.training.domain.ScoreDirection;
import com.ruky.training.domain.ScoreType;
import java.util.UUID;

public record CompetitionEventResponse(
        UUID id,
        String name,
        String description,
        ScoreType scoreType,
        ScoreDirection scoreDirection,
        int orderIndex) {

    public static CompetitionEventResponse from(CompetitionEvent event) {
        return new CompetitionEventResponse(
                event.id(),
                event.name(),
                event.description(),
                event.scoreType(),
                event.scoreDirection(),
                event.orderIndex());
    }
}
