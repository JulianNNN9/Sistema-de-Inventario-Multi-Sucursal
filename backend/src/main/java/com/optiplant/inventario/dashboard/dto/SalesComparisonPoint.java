package com.optiplant.inventario.dashboard.dto;

import java.math.BigDecimal;

/**
 * Un punto de la comparación de ventas (RF-26): {@code periodo} en formato
 * {@code yyyy-MM}. La lista completa va del mes más antiguo al actual.
 */
public record SalesComparisonPoint(
        String periodo,
        BigDecimal totalVentas
) {
}
