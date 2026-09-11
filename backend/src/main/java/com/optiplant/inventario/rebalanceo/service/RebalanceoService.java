package com.optiplant.inventario.rebalanceo.service;

import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.rebalanceo.dto.RebalanceApproveRequest;
import com.optiplant.inventario.rebalanceo.dto.Sugerencia;
import com.optiplant.inventario.rebalanceo.strategy.RebalanceoStrategy;
import com.optiplant.inventario.transferencia.dto.TransferRequest;
import com.optiplant.inventario.transferencia.dto.TransferResponse;
import com.optiplant.inventario.transferencia.entity.Urgencia;
import com.optiplant.inventario.transferencia.service.TransferenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RF-31..RF-34. No persiste sugerencias (Sección 3): {@link #sugerencias()}
 * delega en el {@link RebalanceoStrategy} inyectado (Strategy, apartado 8.2)
 * y recalcula en cada llamada.
 */
@Service
@RequiredArgsConstructor
public class RebalanceoService {

    private final RebalanceoStrategy rebalanceoStrategy;
    private final TransferenciaService transferenciaService;
    private final InventarioSucursalRepository inventarioSucursalRepository;

    @Transactional(readOnly = true)
    public List<Sugerencia> sugerencias() {
        return rebalanceoStrategy.calcular();
    }

    /**
     * RF-33: crea la transferencia reutilizando {@link TransferenciaService}
     * (Facade) — exactamente como si {@code POST /api/v1/transfers} hubiese
     * sido llamado por un ADMIN_GENERAL. Como ninguna sugerencia queda
     * persistida para referenciarla por id (RF-34), la urgencia "heredada" se
     * recalcula aquí con la misma regla de RF-31 sobre el stock ACTUAL del
     * destino, en vez de confiar en un valor que el cliente pudo calcular con
     * datos ya desactualizados entre el {@code GET} y este {@code POST}.
     */
    @Transactional
    public TransferResponse aprobar(RebalanceApproveRequest request) {
        InventarioSucursal destino = inventarioSucursalRepository
                .findByProductoIdAndSucursalId(request.productId(), request.sucursalDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("InventarioSucursal", request.sucursalDestinoId()));

        Urgencia urgencia = destino.getCantidadActual().signum() == 0 ? Urgencia.ALTA : Urgencia.MEDIA;

        TransferRequest transferRequest = new TransferRequest(
                request.productId(), request.cantidadSugerida(), request.sucursalOrigenId(),
                urgencia, request.sucursalDestinoId());
        return transferenciaService.solicitar(transferRequest);
    }
}
