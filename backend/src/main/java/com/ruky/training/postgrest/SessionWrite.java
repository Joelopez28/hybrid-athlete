package com.ruky.training.postgrest;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.WodDivision;
import com.ruky.training.domain.WodFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload for the atomic api.create_session RPC. */
public record SessionWrite(
        UUID athleteId,
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
        List<EntryWrite> entries) {
}
