package com.optiplant.inventario.transferencia.dto;

import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;

import java.time.Instant;

/** Fila del histórico de una transferencia ({@code GET /transfers/{id}/events}, RF-22). */
public record TransferEventResponse(
        Long id,
        EstadoTransferencia estado,
        Instant fecha,
        String comentario
) {
}
