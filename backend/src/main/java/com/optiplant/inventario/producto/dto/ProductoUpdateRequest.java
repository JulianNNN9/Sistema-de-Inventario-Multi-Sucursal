package com.optiplant.inventario.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/v1/products/{id}}. El {@code sku} es la clave de
 * negocio del producto y no se modifica por esta vía.
 */
public record ProductoUpdateRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 160, message = "el nombre no puede superar 160 caracteres")
        String nombre,

        @NotBlank(message = "la unidad de medida base es obligatoria")
        @Size(max = 30, message = "la unidad de medida base no puede superar 30 caracteres")
        String unidadMedidaBase
) {
}
