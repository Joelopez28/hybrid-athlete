package com.ruky.training.web;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Sex;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AthleteResponse(
        UUID id,
        String handle,
        String displayName,
        Sex sex,
        BigDecimal bodyweightKg,
        String country,
        String bio,
        AthleteTier tier,
        List<Discipline> disciplines) {

    public static AthleteResponse from(Athlete athlete) {
        return new AthleteResponse(
                athlete.id(),
                athlete.handle(),
                athlete.displayName(),
                athlete.sex(),
                athlete.bodyweightKg(),
                athlete.country(),
                athlete.bio(),
                athlete.tier(),
                athlete.disciplines());
    }
}
