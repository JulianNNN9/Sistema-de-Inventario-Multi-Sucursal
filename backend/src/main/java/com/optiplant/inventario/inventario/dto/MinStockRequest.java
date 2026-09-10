package com.optiplant.inventario.inventario.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code PUT /api/v1/inventory/{productId}/min-stock} (RF-05).
 * {@code branchId} es opcional: lo ignora todo rol de sucursal (usa el del JWT)
 * y es obligatorio para ADMIN_GENERAL.
 */
public record MinStockRequest(

        @NotNull(message = "el stock mínimo es obligatorio")
        @PositiveOrZero(message = "el stock mínimo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "el stock mínimo admite hasta 10 enteros y 2 decimales")
        BigDecimal stockMinimo,

        Long branchId
) {
}
