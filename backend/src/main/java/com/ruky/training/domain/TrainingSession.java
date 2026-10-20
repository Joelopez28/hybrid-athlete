package com.ruky.training.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record TrainingSession(
        UUID id,
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
        OffsetDateTime createdAt,
        @JsonAlias("session_entries") List<SessionEntry> entries) {

    public TrainingSession {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public BigDecimal volumeKg() {
        return entries.stream()
                .map(SessionEntry::volumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public TrainingSession withSortedEntries() {
        List<SessionEntry> sorted = entries.stream()
                .sorted(Comparator
                        .comparing(SessionEntry::movement, Comparator.nullsLast(String::compareTo))
                        .thenComparing(SessionEntry::setIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        return new TrainingSession(id, athleteId, performedOn, discipline, sessionType, title,
                notes, durationMin, rpe, wodFormat, wodScore, wodDivision, createdAt, sorted);
    }
}
