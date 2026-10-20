package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.domain.WodDivision;
import com.ruky.training.domain.WodFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        LocalDate performedOn,
        Discipline discipline,
        SessionType sessionType,
        String title,
        String notes,
        Integer durationMin,
        BigDecimal rpe,
        WodFormat wodFormat,
        String wodScore,
        WodDivision wodDivision,
        OffsetDateTime createdAt,
        BigDecimal volumeKg,
        List<EntryResponse> entries) {

    public static SessionResponse from(TrainingSession session) {
        return new SessionResponse(
                session.id(),
                session.performedOn(),
                session.discipline(),
                session.sessionType(),
                session.title(),
                session.notes(),
                session.durationMin(),
                session.rpe(),
                session.wodFormat(),
                session.wodScore(),
                session.wodDivision(),
                session.createdAt(),
                session.volumeKg(),
                session.entries().stream().map(EntryResponse::from).toList());
    }
}
