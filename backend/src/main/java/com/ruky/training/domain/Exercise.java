package com.ruky.training.domain;

import java.util.UUID;

public record Exercise(
        UUID id,
        String slug,
        String name,
        Discipline discipline,
        String category,
        String unit,
        boolean benchmark) {
}
