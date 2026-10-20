package com.ruky.training.web;

import java.math.BigDecimal;

public record Totals(
        int sessions,
        int minutes,
        BigDecimal volumeKg,
        int prs) {
}
