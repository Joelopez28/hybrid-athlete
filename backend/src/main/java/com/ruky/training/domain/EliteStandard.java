package com.ruky.training.domain;

import java.math.BigDecimal;

public record EliteStandard(
        String code,
        Discipline discipline,
        String name,
        String metric,
        BigDecimal target,
        String unit,
        String description,
        boolean active) {
}
