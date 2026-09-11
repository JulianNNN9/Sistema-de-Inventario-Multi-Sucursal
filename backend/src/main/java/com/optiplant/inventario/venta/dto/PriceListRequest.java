package com.optiplant.inventario.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/v1/price-lists} (RF-15). {@code branchId} nulo =
 * lista global (solo ADMIN_GENERAL); el resto de roles crea listas de su propia
 * sucursal.
 */
public record PriceListRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar 120 caracteres")
        String nombre,

        Long branchId,

        @NotEmpty(message = "la lista debe tener al menos un ítem")
        @Valid
        List<PriceListItemRequest> items
) {
}
