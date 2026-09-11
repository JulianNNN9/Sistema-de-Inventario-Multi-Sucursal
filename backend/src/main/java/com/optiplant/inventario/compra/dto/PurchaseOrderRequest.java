package com.optiplant.inventario.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/v1/purchase-orders} (RF-08). {@code branchId} es
 * obligatorio para ADMIN_GENERAL; el resto de roles opera sobre su propia
 * sucursal (tomada del JWT). El plazo de pago no se recibe aquí: se toma
 * automáticamente de la frecuencia de pago configurada en el proveedor.
 */
public record PurchaseOrderRequest(

        @NotNull(message = "debes seleccionar un proveedor")
        Long supplierId,

        Long branchId,

        @NotEmpty(message = "la orden debe tener al menos una línea")
        @Valid
        List<PurchaseOrderLineRequest> lineas
) {
}
