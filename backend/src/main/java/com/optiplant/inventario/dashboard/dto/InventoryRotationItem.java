package com.optiplant.inventario.dashboard.dto;

import java.math.BigDecimal;

/** Un producto y la suma de unidades retiradas en la ventana de rotación (RF-27). */
public record InventoryRotationItem(
        Long productId,
        String sku,
        String nombre,
        BigDecimal cantidadRetirada
) {
}
