package com.optiplant.inventario.inventario.dto;

import java.math.BigDecimal;

/**
 * Existencias de un producto en una sucursal (RF-02, RF-05). {@code alertaStockBajo}
 * = {@code cantidadActual < stockMinimo}.
 */
public record InventarioResponse(
        Long productId,
        String sku,
        String productoNombre,
        Long branchId,
        String sucursalNombre,
        BigDecimal cantidadActual,
        BigDecimal stockMinimo,
        BigDecimal costoPromedioPonderado,
        boolean alertaStockBajo
) {
}
