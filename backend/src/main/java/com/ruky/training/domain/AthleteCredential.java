package com.ruky.training.domain;

import java.util.UUID;

public record AthleteCredential(
        UUID athleteId,
        String email,
        String passwordHash,
        Athlete athlete) {
}
