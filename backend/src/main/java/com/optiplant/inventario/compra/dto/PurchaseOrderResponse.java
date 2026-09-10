package com.optiplant.inventario.compra.dto;

import com.optiplant.inventario.compra.entity.EstadoOrdenCompra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Respuesta completa de una orden de compra ({@code GET /purchase-orders/{id}}). */
public record PurchaseOrderResponse(
        Long id,
        Long supplierId,
        String supplierNombre,
        Long branchId,
        String sucursalNombre,
        Instant fecha,
        EstadoOrdenCompra estado,
        String plazoPago,
        BigDecimal total,
        List<PurchaseOrderLineResponse> lineas
) {
}
