package com.optiplant.inventario.dashboard.dto;

import java.math.BigDecimal;

/** RF-29: producto cuya {@code cantidadActual} ya llegó (o bajó) del {@code stockMinimo}. */
public record RestockAlert(
        Long productId,
        String sku,
        String productoNombre,
        Long sucursalId,
        String sucursalNombre,
        BigDecimal cantidadActual,
        BigDecimal stockMinimo
) {
}
