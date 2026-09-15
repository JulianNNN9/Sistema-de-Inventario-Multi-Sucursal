package com.optiplant.inventario.transferencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code PUT /api/v1/transfers/{id}/resolve} (RF-21, GERENTE_SUCURSAL de destino). */
public record ResolveRequest(

        @NotNull(message = "el tratamiento es obligatorio")
        TratamientoFaltante tratamiento,

        /** PQRS: qué pasó con el faltante y por qué se resuelve así; queda en el historial de la transferencia. */
        @NotBlank(message = "debes describir qué pasó con el faltante")
        @Size(max = 500, message = "la descripción no puede superar 500 caracteres")
        String detalle
) {
}
