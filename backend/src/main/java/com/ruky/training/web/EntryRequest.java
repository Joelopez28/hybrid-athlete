package com.ruky.training.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record EntryRequest(
        UUID exerciseId,

        @Size(max = 120, message = "El movimiento no puede superar 120 caracteres")
        String movement,

        @Min(value = 1, message = "El número de serie va de 1 a 100")
        @Max(value = 100, message = "El número de serie va de 1 a 100")
        Integer setIndex,

        @Min(value = 1, message = "Las repeticiones van de 1 a 1000")
        @Max(value = 1000, message = "Las repeticiones van de 1 a 1000")
        Integer reps,

        @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
        @DecimalMax(value = "1000.0", message = "El peso no puede superar 1000 kg")
        BigDecimal weightKg,

        @DecimalMin(value = "0.0", message = "Los segundos no pueden ser negativos")
        @DecimalMax(value = "7200.0", message = "Los segundos no pueden superar 7200")
        BigDecimal seconds,

        @DecimalMin(value = "1.0", message = "El RPE va de 1 a 10")
        @DecimalMax(value = "10.0", message = "El RPE va de 1 a 10")
        BigDecimal rpe) {
}
