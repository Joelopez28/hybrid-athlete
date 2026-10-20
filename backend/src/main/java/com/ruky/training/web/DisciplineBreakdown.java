package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import java.math.BigDecimal;

public record DisciplineBreakdown(
        Discipline discipline,
        int sessions,
        BigDecimal volumeKg) {
}
