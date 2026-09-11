package com.optiplant.inventario.dashboard.dto;

import java.math.BigDecimal;

/**
 * RF-30 (solo ADMIN_GENERAL): por sucursal, ventas totales del mes actual y
 * rotación agregada (RETIRO) de los últimos 30 días — mismas ventanas que
 * RF-26 y RF-27, aplicadas aquí a todas las sucursales a la vez.
 */
public record BranchComparisonRow(
        Long sucursalId,
        String sucursalNombre,
        BigDecimal ventasTotales,
        BigDecimal rotacionTotal
) {
}
