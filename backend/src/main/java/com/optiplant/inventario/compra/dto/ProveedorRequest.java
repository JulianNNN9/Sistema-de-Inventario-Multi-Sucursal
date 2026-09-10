package com.optiplant.inventario.compra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/suppliers}. */
public record ProveedorRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 160, message = "el nombre no puede superar 160 caracteres")
        String nombre,

        String condiciones
) {
}
