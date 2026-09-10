package com.optiplant.inventario.sucursal.dto;

/** Respuesta de los endpoints de {@code /api/v1/branches}. */
public record SucursalResponse(
        Long id,
        String nombre,
        String ciudad
) {
}
