package com.optiplant.inventario.venta.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Fila del histórico de ventas ({@code GET /api/v1/sales}). */
public record SaleSummaryResponse(
        Long id,
        Long branchId,
        String sucursalNombre,
        String usuarioNombre,
        Instant fecha,
        BigDecimal total
) {
}
