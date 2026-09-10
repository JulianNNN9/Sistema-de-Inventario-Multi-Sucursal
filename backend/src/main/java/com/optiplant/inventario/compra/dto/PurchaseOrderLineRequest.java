package com.optiplant.inventario.compra.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Línea de {@code POST /api/v1/purchase-orders} (RF-09). */
public record PurchaseOrderLineRequest(

        @NotNull(message = "productId es obligatorio")
        Long productId,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidad,

        @NotNull(message = "el precio unitario es obligatorio")
        @PositiveOrZero(message = "el precio unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "el precio unitario admite hasta 10 enteros y 2 decimales")
        BigDecimal precioUnitario,

        @PositiveOrZero(message = "el descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "el descuento no puede superar 100")
        @Digits(integer = 3, fraction = 2, message = "el descuento admite hasta 3 enteros y 2 decimales")
        BigDecimal descuento
) {
}
