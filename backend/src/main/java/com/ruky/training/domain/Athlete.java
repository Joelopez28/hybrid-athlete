package com.ruky.training.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Athlete(
        UUID id,
        String handle,
        String displayName,
        Sex sex,
        LocalDate birthDate,
        java.math.BigDecimal bodyweightKg,
        String country,
        String bio,
        AthleteTier tier,
        List<Discipline> disciplines) {

    public Athlete {
        disciplines = disciplines == null ? List.of() : List.copyOf(disciplines);
    }
}
