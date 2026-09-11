package com.optiplant.inventario.venta.dto;

import java.math.BigDecimal;

/** Línea del comprobante de venta. {@code subtotal} = neto (con descuento). */
public record SaleLineResponse(
        Long productId,
        String sku,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal
) {
}
