package com.ruky.training.domain;

import java.util.UUID;

public record CompetitionRegistration(
        UUID id,
        UUID competitionId,
        UUID divisionId,
        UUID athleteId,
        RegistrationStatus status,
        Athlete athlete) {
}
