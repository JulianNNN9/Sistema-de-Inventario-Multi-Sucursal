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
        BigDecimal costo,
        EstadoTransferencia estado,
        Urgencia urgencia,
        String transportista,
        Instant fechaEstimadaLlegada,
        Instant fechaRealLlegada,
        /** El modelo no tiene un estado "APROBADA" propio (Sección 3): esto indica si,
         * estando PENDIENTE, ya fue aprobada por el origen y está lista para despacho. */
        boolean aprobada
) {
}
