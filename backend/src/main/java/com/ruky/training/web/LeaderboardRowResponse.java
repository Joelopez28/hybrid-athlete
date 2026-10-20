package com.ruky.training.web;

import com.ruky.training.domain.AthleteTier;
import java.util.List;
import java.util.UUID;

public record LeaderboardRowResponse(
        int rank,
        UUID athleteId,
        String displayName,
        String handle,
        AthleteTier tier,
        int totalPoints,
        int firstPlaces,
        List<EventScoreResponse> eventScores) {

    public LeaderboardRowResponse {
        eventScores = List.copyOf(eventScores);
    }
}
