package com.optiplant.inventario.sucursal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/branches}. */
public record SucursalRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 120, message = "la ciudad no puede superar 120 caracteres")
        String ciudad
) {
}
