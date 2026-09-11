package com.optiplant.inventario.rebalanceo.controller;

import com.optiplant.inventario.rebalanceo.dto.RebalanceApproveRequest;
import com.optiplant.inventario.rebalanceo.dto.Sugerencia;
import com.optiplant.inventario.rebalanceo.service.RebalanceoService;
import com.optiplant.inventario.transferencia.dto.TransferResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recomendador de rebalanceo de inventario (RF-31..RF-34, Módulo 7). Solo
 * ADMIN_GENERAL (Sección 4.2).
 */
@Tag(name = "Rebalanceo", description = "Sugerencias de traslado entre sucursales por déficit/superávit (Módulo 7).")
@RestController
@RequestMapping("/api/v1/rebalance-suggestions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_GENERAL')")
public class RebalanceoController {

    private final RebalanceoService rebalanceoService;

    @GetMapping
    public List<Sugerencia> listar() {
        return rebalanceoService.sugerencias();
    }

    @PostMapping("/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse aprobar(@Valid @RequestBody RebalanceApproveRequest request) {
        return rebalanceoService.aprobar(request);
    }
}
