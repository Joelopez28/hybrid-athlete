package com.ruky.training.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Ingresá tu email o usuario") String identifier,
        @NotBlank(message = "Ingresá tu contraseña") String password) {
}
