package com.optiplant.inventario.venta.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Comprobante de venta ({@code GET /api/v1/sales/{id}}, RF-16). */
public record SaleResponse(
        Long id,
        Long branchId,
        String sucursalNombre,
        Long usuarioId,
        String usuarioNombre,
        Instant fecha,
        BigDecimal total,
        List<SaleLineResponse> lineas
) {
}
