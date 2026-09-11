package com.optiplant.inventario.rebalanceo.dto;

import com.optiplant.inventario.transferencia.entity.Urgencia;

import java.math.BigDecimal;

/**
 * Sugerencia de rebalanceo entre dos sucursales para un producto (RF-31). No
 * se persiste (Sección 3): se recalcula en cada {@code GET}. Los campos
 * mínimos exigidos por el roadmap son {@code productId}, {@code
 * cantidadSugerida}, {@code sucursalOrigenId}, {@code sucursalDestinoId} y
 * {@code urgencia}; el resto son nombres legibles para el frontend (mismo
 * criterio que {@code TransferResponse}).
 */
public record Sugerencia(
        Long productId,
        String sku,
        String productoNombre,
        BigDecimal cantidadSugerida,
        Long sucursalOrigenId,
        String sucursalOrigenNombre,
        Long sucursalDestinoId,
        String sucursalDestinoNombre,
        Urgencia urgencia
) {
}
