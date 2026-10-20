package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Sex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record RegisterAthleteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
        String displayName,

        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "[a-z0-9_]{3,30}",
                message = "El usuario debe tener 3-30 caracteres: minúsculas, números o _")
        String handle,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        Sex sex,

        @DecimalMin(value = "30.0", message = "El peso debe ser mayor a 30 kg")
        @DecimalMax(value = "250.0", message = "El peso debe ser menor a 250 kg")
        BigDecimal bodyweightKg,

        @Size(max = 80, message = "El país no puede superar 80 caracteres")
        String country,

        List<Discipline> disciplines) {
}
