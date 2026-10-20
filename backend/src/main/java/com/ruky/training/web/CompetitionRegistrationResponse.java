package com.ruky.training.web;

import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.RegistrationStatus;
import java.util.UUID;

public record CompetitionRegistrationResponse(
        UUID id,
        UUID divisionId,
        String divisionName,
        RegistrationStatus status) {

    public static CompetitionRegistrationResponse from(CompetitionRegistration registration,
                                                        String divisionName) {
        return new CompetitionRegistrationResponse(
                registration.id(),
                registration.divisionId(),
                divisionName,
                registration.status());
    }
}
