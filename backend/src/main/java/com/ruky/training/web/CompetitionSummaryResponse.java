package com.ruky.training.web;

import com.ruky.training.domain.Competition;
import com.ruky.training.domain.CompetitionStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CompetitionSummaryResponse(
        UUID id,
        String slug,
        String name,
        String description,
        CompetitionStatus status,
        LocalDate startsOn,
        LocalDate endsOn,
        String location) {

    public static CompetitionSummaryResponse from(Competition competition) {
        return new CompetitionSummaryResponse(
                competition.id(),
                competition.slug(),
                competition.name(),
                competition.description(),
                competition.status(),
                competition.startsOn(),
                competition.endsOn(),
                competition.location());
    }
}
