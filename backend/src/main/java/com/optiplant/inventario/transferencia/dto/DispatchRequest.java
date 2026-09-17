package com.optiplant.inventario.transferencia.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cuerpo de {@code PUT /api/v1/transfers/{id}/dispatch} (RF-18, RF-19).
 * {@code costo} (RF-23) se captura aquí porque es en el despacho cuando se
 * conoce el costo real del envío (transportista, cantidad enviada); en la
 * solicitud todavía no existe.
 */
public record DispatchRequest(

        @NotNull(message = "la cantidad enviada es obligatoria")
        @Positive(message = "la cantidad enviada debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad enviada admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidadEnviada,

        @NotNull(message = "el transportista es obligatorio")
        Long transportistaId,

        @NotNull(message = "la fecha estimada de llegada es obligatoria")
        Instant fechaEstimadaLlegada,

        @NotNull(message = "el costo del envío es obligatorio")
        @PositiveOrZero(message = "el costo del envío no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "el costo admite hasta 10 enteros y 2 decimales")
        BigDecimal costo
) {
}
