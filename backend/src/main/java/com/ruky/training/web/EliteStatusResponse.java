package com.ruky.training.web;

import com.ruky.training.domain.AthleteTier;
import java.util.List;

public record EliteStatusResponse(
        AthleteResponse athlete,
        AthleteTier tier,
        boolean qualified,
        int metCount,
        int requiredCount,
        List<EliteCriterionResponse> standards,
        String message) {

    public EliteStatusResponse {
        standards = List.copyOf(standards);
    }
}
