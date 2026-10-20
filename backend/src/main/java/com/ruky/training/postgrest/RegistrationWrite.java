package com.ruky.training.postgrest;

import com.ruky.training.domain.RegistrationStatus;
import java.util.UUID;

public record RegistrationWrite(
        UUID competitionId,
        UUID divisionId,
        UUID athleteId,
        RegistrationStatus status) {
}
