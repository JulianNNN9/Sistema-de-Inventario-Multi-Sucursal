package com.optiplant.inventario.venta.dto;

import java.util.List;

/** {@code branchId}/{@code sucursalNombre} nulos = lista global. */
public record PriceListResponse(
        Long id,
        String nombre,
        Long branchId,
        String sucursalNombre,
        List<PriceListItemResponse> items
) {
}
