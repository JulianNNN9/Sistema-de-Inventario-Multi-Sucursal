package com.optiplant.inventario.transferencia.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PUT /api/v1/transfers/{id}/approve} (Sección 4.2, GERENTE_SUCURSAL de origen). */
public record ApproveRequest(

        @NotNull(message = "el campo aprobado es obligatorio")
        Boolean aprobado
) {
}
