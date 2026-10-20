package com.ruky.training.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterRequest(@NotNull(message = "Elegí una división") UUID divisionId) {
}
