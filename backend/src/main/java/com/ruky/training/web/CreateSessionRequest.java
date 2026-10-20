package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.WodDivision;
import com.ruky.training.domain.WodFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateSessionRequest(
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate performedOn,

        @NotNull(message = "La disciplina es obligatoria")
        Discipline discipline,

        @NotNull(message = "El tipo de sesión es obligatorio")
        SessionType sessionType,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120, message = "El título no puede superar 120 caracteres")
        String title,

        @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres")
        String notes,

        @Min(value = 1, message = "La duración debe ser al menos 1 minuto")
        @Max(value = 600, message = "La duración no puede superar 600 minutos")
        Integer durationMin,

        @DecimalMin(value = "1.0", message = "El RPE va de 1 a 10")
        @DecimalMax(value = "10.0", message = "El RPE va de 1 a 10")
        BigDecimal rpe,

        WodFormat wodFormat,

        @Size(max = 40, message = "El resultado no puede superar 40 caracteres")
        String wodScore,

        WodDivision wodDivision,

        @Valid List<EntryRequest> entries) {
}
