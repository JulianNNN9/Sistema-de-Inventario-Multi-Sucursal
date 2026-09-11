package com.optiplant.inventario.dashboard.dto;

import java.util.List;

/**
 * RF-27: top 5 productos de mayor y de menor rotación (suma de {@code cantidad}
 * en movimientos RETIRO de los últimos 30 días). Solo incluye productos con al
 * menos un retiro en la ventana — uno sin movimientos no tiene "rotación" que
 * ordenar, ni alta ni baja.
 */
public record InventoryRotationResponse(
        List<InventoryRotationItem> mayorRotacion,
        List<InventoryRotationItem> menorRotacion
) {
}
