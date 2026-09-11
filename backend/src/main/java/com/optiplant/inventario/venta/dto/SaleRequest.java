package com.optiplant.inventario.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/v1/sales} (RF-13). {@code branchId} es obligatorio
 * para ADMIN_GENERAL; el resto de roles vende sobre su propia sucursal (JWT).
 */
public record SaleRequest(

        Long branchId,

        @NotEmpty(message = "la venta debe tener al menos una línea")
        @Valid
        List<SaleLineRequest> lineas
) {
}
