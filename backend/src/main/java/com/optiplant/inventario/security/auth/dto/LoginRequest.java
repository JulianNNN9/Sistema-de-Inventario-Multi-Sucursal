package com.optiplant.inventario.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de {@code POST /api/v1/auth/login} (Sección 4.1). */
public record LoginRequest(

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        String email,

        @NotBlank(message = "la contraseña es obligatoria")
        String password
) {
}
