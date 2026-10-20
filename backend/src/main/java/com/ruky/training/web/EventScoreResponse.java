package com.ruky.training.web;

import java.util.UUID;

public record EventScoreResponse(
        UUID eventId,
        Integer place,
        int points,
        String displayScore) {
}
