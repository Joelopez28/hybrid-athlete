package com.ruky.training.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubmitResultRequest(
        @NotNull(message = "El resultado es obligatorio")
        @DecimalMin(value = "0.0", message = "El resultado no puede ser negativo")
        BigDecimal rawScore,

        @Size(max = 40, message = "El resultado visible no puede superar 40 caracteres")
        String displayScore,

        @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
        String notes) {
}
