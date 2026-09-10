package com.optiplant.inventario.producto.dto;

import java.math.BigDecimal;

/** Respuesta de {@code /api/v1/products/{id}/units} (RF-06). */
public record ProductoUnidadResponse(
        Long id,
        Long productoId,
        String nombreUnidad,
        BigDecimal factorConversion
) {
}
