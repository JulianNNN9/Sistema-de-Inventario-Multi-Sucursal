package com.optiplant.inventario.transferencia.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PUT /api/v1/transfers/{id}/resolve} (RF-21, GERENTE_SUCURSAL de destino). */
public record ResolveRequest(

        @NotNull(message = "el tratamiento es obligatorio")
        TratamientoFaltante tratamiento
) {
}
