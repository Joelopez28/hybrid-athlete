package com.ruky.training.web;

import com.ruky.training.domain.AthleteTier;
import java.math.BigDecimal;
import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID athleteId,
        String displayName,
        String handle,
        AthleteTier tier,
        BigDecimal value,
        int sessions,
        boolean me) {
}
