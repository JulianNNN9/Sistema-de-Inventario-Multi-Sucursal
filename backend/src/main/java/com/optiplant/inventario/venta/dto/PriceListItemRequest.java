package com.optiplant.inventario.venta.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Ítem de {@code POST /api/v1/price-lists} (RF-15). */
public record PriceListItemRequest(

        @NotNull(message = "productId es obligatorio")
        Long productId,

        @NotNull(message = "el precio es obligatorio")
        @Positive(message = "el precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "el precio admite hasta 10 enteros y 2 decimales")
        BigDecimal precio
) {
}
