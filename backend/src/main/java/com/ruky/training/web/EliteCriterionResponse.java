package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import java.math.BigDecimal;

public record EliteCriterionResponse(
        String code,
        Discipline discipline,
        String name,
        BigDecimal target,
        BigDecimal actual,
        String unit,
        boolean met,
        BigDecimal progress,
        String detail) {
}
