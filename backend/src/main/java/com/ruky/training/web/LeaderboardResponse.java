package com.ruky.training.web;

import java.util.List;
import java.util.UUID;

public record LeaderboardResponse(
        UUID divisionId,
        String divisionName,
        List<LeaderboardRowResponse> rows) {

    public LeaderboardResponse {
        rows = List.copyOf(rows);
    }
}
