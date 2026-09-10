package com.optiplant.inventario.producto.dto;

/** Respuesta de los endpoints de {@code /api/v1/products}. */
public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        String unidadMedidaBase
) {
}
