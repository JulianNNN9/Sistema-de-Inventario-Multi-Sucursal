package com.optiplant.inventario.compra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/suppliers} y {@code PUT /api/v1/suppliers/{id}}. */
public record ProveedorRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 160, message = "el nombre no puede superar 160 caracteres")
        String nombre,

        @NotBlank(message = "la frecuencia de pago es obligatoria")
        @Size(max = 60, message = "la frecuencia de pago no puede superar 60 caracteres")
        String frecuenciaPago,

        String condiciones
) {
}
