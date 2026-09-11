package com.optiplant.inventario.transferencia.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** Cuerpo de {@code PUT /api/v1/transfers/{id}/dispatch} (RF-18, RF-19). */
public record DispatchRequest(

        @NotNull(message = "la cantidad enviada es obligatoria")
        @Positive(message = "la cantidad enviada debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad enviada admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidadEnviada,

        @NotBlank(message = "el transportista es obligatorio")
        @Size(max = 120, message = "el transportista no puede superar 120 caracteres")
        String transportista,

        @NotNull(message = "la fecha estimada de llegada es obligatoria")
        Instant fechaEstimadaLlegada
) {
}
