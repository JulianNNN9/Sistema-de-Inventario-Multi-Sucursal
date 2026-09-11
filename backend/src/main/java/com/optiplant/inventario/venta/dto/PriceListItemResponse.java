package com.optiplant.inventario.venta.dto;

import java.math.BigDecimal;

public record PriceListItemResponse(
        Long productId,
        String sku,
        String productoNombre,
        BigDecimal precio
) {
}
