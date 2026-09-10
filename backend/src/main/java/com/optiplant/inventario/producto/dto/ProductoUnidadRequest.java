package com.optiplant.inventario.producto.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Cuerpo de {@code POST /api/v1/products/{id}/units} (RF-06). */
public record ProductoUnidadRequest(

        @NotBlank(message = "el nombre de la unidad es obligatorio")
        String nombreUnidad,

        @NotNull(message = "el factor de conversión es obligatorio")
        @Positive(message = "el factor de conversión debe ser mayor que cero")
        @Digits(integer = 8, fraction = 4,
                message = "el factor de conversión admite hasta 8 enteros y 4 decimales")
        BigDecimal factorConversion
) {
}
