package com.optiplant.inventario.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code PUT /api/v1/price-lists/{id}} (RF-15). El {@code branchId} es
 * la clave de negocio de la lista (global vs. de sucursal) y no se modifica por
 * esta vía, igual que el {@code sku} de un producto.
 */
public record PriceListUpdateRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar 120 caracteres")
        String nombre,

        @NotEmpty(message = "la lista debe tener al menos un ítem")
        @Valid
        List<PriceListItemRequest> items
) {
}
