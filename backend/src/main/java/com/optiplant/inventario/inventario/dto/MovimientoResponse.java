package com.optiplant.inventario.inventario.dto;

import com.optiplant.inventario.inventario.entity.MotivoMovimiento;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Respuesta de {@code POST /api/v1/inventory-movements}. {@code alertaStockBajo}
 * es informativo (no se persiste): {@code true} si tras aplicar el movimiento el
 * stock quedó por debajo del mínimo configurado (RF-05).
 */
public record MovimientoResponse(
        Long movimientoId,
        Long productId,
        Long branchId,
        TipoMovimiento tipo,
        MotivoMovimiento motivo,
        BigDecimal cantidad,
        BigDecimal cantidadActual,
        BigDecimal stockMinimo,
        boolean alertaStockBajo,
        Instant fecha
) {
}
