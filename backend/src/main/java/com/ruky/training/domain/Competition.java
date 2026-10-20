package com.ruky.training.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Competition(
        UUID id,
        String slug,
        String name,
        String description,
        CompetitionStatus status,
        LocalDate startsOn,
        LocalDate endsOn,
        String location) {
}
