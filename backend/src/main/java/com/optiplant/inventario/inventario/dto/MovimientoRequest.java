package com.optiplant.inventario.inventario.dto;

import com.optiplant.inventario.inventario.entity.MotivoMovimiento;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Cuerpo de {@code POST /api/v1/inventory-movements} (RF-03, RF-04, RF-07). */
public record MovimientoRequest(

        @NotNull(message = "productId es obligatorio")
        Long productId,

        @NotNull(message = "branchId es obligatorio")
        Long branchId,

        @NotNull(message = "tipo es obligatorio")
        TipoMovimiento tipo,

        @NotNull(message = "motivo es obligatorio")
        MotivoMovimiento motivo,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidad
) {
}
