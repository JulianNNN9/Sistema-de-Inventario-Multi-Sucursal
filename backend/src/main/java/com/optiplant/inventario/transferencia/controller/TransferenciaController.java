package com.optiplant.inventario.transferencia.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.transferencia.dto.ApproveRequest;
import com.optiplant.inventario.transferencia.dto.DispatchRequest;
import com.optiplant.inventario.transferencia.dto.ReceiveRequest;
import com.optiplant.inventario.transferencia.dto.ResolveRequest;
import com.optiplant.inventario.transferencia.dto.TransferEventResponse;
import com.optiplant.inventario.transferencia.dto.TransferRequest;
import com.optiplant.inventario.transferencia.dto.TransferResponse;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.service.TransferenciaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ciclo de vida de transferencias entre sucursales (RF-17..RF-21). Autorización
 * por acción según la Sección 4.2 — nótese que, a diferencia del resto de
 * módulos, ADMIN_GENERAL NO puede aprobar/despachar/recibir/resolver (la matriz
 * lo marca explícitamente ✘ en esas 4 filas).
 */
@Tag(name = "Transferencias", description = "Ciclo de vida completo de una transferencia entre sucursales (Módulo 4).")
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','OPERADOR_INVENTARIO')")
    public TransferResponse solicitar(@Valid @RequestBody TransferRequest request) {
        return transferenciaService.solicitar(request);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('GERENTE_SUCURSAL')")
    public TransferResponse aprobar(@PathVariable Long id, @Valid @RequestBody ApproveRequest request) {
        return transferenciaService.aprobar(id, request);
    }

    @PutMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public TransferResponse despachar(@PathVariable Long id, @Valid @RequestBody DispatchRequest request) {
        return transferenciaService.despachar(id, request);
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public TransferResponse recibir(@PathVariable Long id, @Valid @RequestBody ReceiveRequest request) {
        return transferenciaService.recibir(id, request);
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('GERENTE_SUCURSAL')")
    public TransferResponse resolver(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return transferenciaService.resolver(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<TransferResponse> listar(
            @RequestParam(name = "estado", required = false) EstadoTransferencia estado,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "sort", required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        // page/size de Pageable; "sort" se interpreta aparte (RF-23: priority|cost|time,
        // no son propiedades JPA reales de Transferencia salvo "time").
        return transferenciaService.listar(estado, branchId, sort,
                pageable.getPageNumber(), pageable.getPageSize());
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<TransferEventResponse> eventos(
            @PathVariable Long id,
            @PageableDefault(size = 50, sort = "fecha", direction = Sort.Direction.ASC) Pageable pageable) {
        return transferenciaService.eventos(id, pageable);
    }
}
