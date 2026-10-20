package com.ruky.training.web;

import java.math.BigDecimal;
import java.util.List;

public record StatsResponse(
        AthleteResponse athlete,
        Totals totals,
        List<WeeklyPoint> weekly,
        List<PersonalRecord> personalRecords,
        List<DisciplineBreakdown> disciplines) {

    public StatsResponse {
        weekly = List.copyOf(weekly);
        personalRecords = List.copyOf(personalRecords);
        disciplines = List.copyOf(disciplines);
    }
}
