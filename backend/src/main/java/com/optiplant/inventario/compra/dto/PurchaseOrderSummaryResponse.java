package com.optiplant.inventario.compra.dto;

import com.optiplant.inventario.compra.entity.EstadoOrdenCompra;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fila del histórico de compras ({@code GET /purchase-orders}, RF-11). El
 * {@code total} se calcula en base de datos (subconsulta agregada, RNF-01).
 */
public record PurchaseOrderSummaryResponse(
        Long id,
        Long supplierId,
        String supplierNombre,
        Long branchId,
        String sucursalNombre,
        Instant fecha,
        EstadoOrdenCompra estado,
        BigDecimal total
) {
}
