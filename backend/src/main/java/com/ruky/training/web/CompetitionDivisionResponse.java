package com.ruky.training.web;

import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.CompetitionDivision;
import java.util.UUID;

public record CompetitionDivisionResponse(
        UUID id,
        String name,
        AthleteTier tier,
        Integer capacity,
        int participants) {

    public static CompetitionDivisionResponse from(CompetitionDivision division, int participants) {
        return new CompetitionDivisionResponse(
                division.id(),
                division.name(),
                division.tier(),
                division.capacity(),
                participants);
    }
}
