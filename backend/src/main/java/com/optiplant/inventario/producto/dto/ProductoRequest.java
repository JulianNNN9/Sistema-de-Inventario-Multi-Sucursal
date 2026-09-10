package com.optiplant.inventario.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/products}. */
public record ProductoRequest(

        @NotBlank(message = "el sku es obligatorio")
        @Size(max = 60, message = "el sku no puede superar 60 caracteres")
        String sku,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 160, message = "el nombre no puede superar 160 caracteres")
        String nombre,

        @NotBlank(message = "la unidad de medida base es obligatoria")
        @Size(max = 30, message = "la unidad de medida base no puede superar 30 caracteres")
        String unidadMedidaBase
) {
}
