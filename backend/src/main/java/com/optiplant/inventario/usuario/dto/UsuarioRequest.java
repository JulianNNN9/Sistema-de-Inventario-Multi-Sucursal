package com.optiplant.inventario.usuario.dto;

import com.optiplant.inventario.usuario.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/users}. */
public record UsuarioRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar 120 caracteres")
        String nombre,

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        @Size(max = 160, message = "el email no puede superar 160 caracteres")
        String email,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres")
        String password,

        @NotNull(message = "el rol es obligatorio")
        Rol rol,

        Long sucursalId
) {
}
