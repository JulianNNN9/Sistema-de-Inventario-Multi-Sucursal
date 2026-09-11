package com.optiplant.inventario.transferencia.dto;

import com.optiplant.inventario.transferencia.entity.Urgencia;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST /api/v1/transfers} (RF-17). {@code sucursalDestinoId}
 * es obligatorio para ADMIN_GENERAL; OPERADOR_INVENTARIO solicita siempre para
 * su propia sucursal (JWT).
 */
public record TransferRequest(

        @NotNull(message = "productId es obligatorio")
        Long productId,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "la cantidad admite hasta 10 enteros y 2 decimales")
        BigDecimal cantidad,

        @NotNull(message = "sucursalOrigenId es obligatorio")
        Long sucursalOrigenId,

        @NotNull(message = "la urgencia es obligatoria")
        Urgencia urgencia,

        Long sucursalDestinoId
) {
}
