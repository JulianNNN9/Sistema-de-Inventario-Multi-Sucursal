package com.optiplant.inventario.inventario.dto;

import com.optiplant.inventario.inventario.entity.MotivoMovimiento;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Cuerpo de {@code POST /api/v1/inventory-movements} (RF-03, RF-04, RF-07). */
public record MovimientoRequest(

        @NotNull(message = "debes seleccionar un producto")
        Long productId,

        @NotNull(message = "debes seleccionar una sucursal")
        Long branchId,

        @NotNull(message = "debes seleccionar el tipo de movimiento")
        TipoMovimiento tipo,

        @NotNull(message = "debes seleccionar el motivo del movimiento")
        MotivoMovimiento motivo,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidad
) {
}
