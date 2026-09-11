package com.optiplant.inventario.rebalanceo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST /api/v1/rebalance-suggestions/approve} (RF-33) — misma
 * forma que una {@link Sugerencia}, sin {@code urgencia}: no hay una
 * sugerencia persistida que referenciar por id (RF-34), así que el Service
 * la vuelve a calcular sobre el stock actual del destino en vez de confiar en
 * un valor que el cliente pudo obtener con datos ya desactualizados.
 */
public record RebalanceApproveRequest(

        @NotNull(message = "productId es obligatorio")
        Long productId,

        @NotNull(message = "la cantidad sugerida es obligatoria")
        @DecimalMin(value = "0.01", message = "la cantidad sugerida debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidadSugerida,

        @NotNull(message = "sucursalOrigenId es obligatorio")
        Long sucursalOrigenId,

        @NotNull(message = "sucursalDestinoId es obligatorio")
        Long sucursalDestinoId
) {
}
