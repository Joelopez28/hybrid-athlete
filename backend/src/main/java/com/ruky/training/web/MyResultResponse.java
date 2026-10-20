package com.ruky.training.web;

import java.math.BigDecimal;
import java.util.UUID;

public record MyResultResponse(
        UUID eventId,
        BigDecimal rawScore,
        String displayScore,
        String notes,
        Integer place,
        int points) {
}
