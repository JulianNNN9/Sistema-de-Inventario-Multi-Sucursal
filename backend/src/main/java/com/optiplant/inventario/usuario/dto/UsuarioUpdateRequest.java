package com.optiplant.inventario.usuario.dto;

import com.optiplant.inventario.usuario.entity.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/v1/users/{id}}. El {@code email} es la identidad de
 * acceso y no se modifica por esta vía. {@code password} es opcional: si viene,
 * se re-hashea.
 */
public record UsuarioUpdateRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar 120 caracteres")
        String nombre,

        @NotNull(message = "el rol es obligatorio")
        Rol rol,

        Long sucursalId,

        @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres")
        String password
) {
}
