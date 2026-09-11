package com.optiplant.inventario.transferencia.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Cuerpo de {@code PUT /api/v1/transfers/{id}/receive} (RF-20, RF-21). */
public record ReceiveRequest(

        @NotNull(message = "la cantidad recibida es obligatoria")
        @PositiveOrZero(message = "la cantidad recibida no puede ser negativa")
        @Digits(integer = 10, fraction = 2, message = "la cantidad recibida admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidadRecibida
) {
}
