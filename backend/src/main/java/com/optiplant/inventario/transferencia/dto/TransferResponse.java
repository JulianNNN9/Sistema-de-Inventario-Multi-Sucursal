package com.optiplant.inventario.transferencia.dto;

import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.entity.Urgencia;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        Long productId,
        String sku,
        String productoNombre,
        Long sucursalOrigenId,
        String sucursalOrigenNombre,
        Long sucursalDestinoId,
        String sucursalDestinoNombre,
        BigDecimal cantidadSolicitada,
        BigDecimal cantidadEnviada,
        BigDecimal cantidadRecibida,
        EstadoTransferencia estado,
        Urgencia urgencia,
        String transportista,
        Instant fechaEstimadaLlegada,
        Instant fechaRealLlegada
) {
}
