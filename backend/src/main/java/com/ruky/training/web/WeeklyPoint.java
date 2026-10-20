package com.ruky.training.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyPoint(
        LocalDate weekStart,
        int sessions,
        int minutes,
        BigDecimal volumeKg) {
}
