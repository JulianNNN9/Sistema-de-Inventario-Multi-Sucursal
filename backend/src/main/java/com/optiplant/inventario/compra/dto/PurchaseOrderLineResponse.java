package com.optiplant.inventario.compra.dto;

import java.math.BigDecimal;

/** Línea de una orden de compra en la respuesta. {@code subtotal} = neto (con descuento aplicado). */
public record PurchaseOrderLineResponse(
        Long productId,
        String sku,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal
) {
}
