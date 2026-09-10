package com.optiplant.inventario.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/v1/purchase-orders} (RF-08). {@code branchId} es
 * obligatorio para ADMIN_GENERAL; el resto de roles opera sobre su propia
 * sucursal (tomada del JWT).
 */
public record PurchaseOrderRequest(

        @NotNull(message = "supplierId es obligatorio")
        Long supplierId,

        Long branchId,

        @Size(max = 60, message = "el plazo de pago no puede superar 60 caracteres")
        String plazoPago,

        @NotEmpty(message = "la orden debe tener al menos una línea")
        @Valid
        List<PurchaseOrderLineRequest> lineas
) {
}
