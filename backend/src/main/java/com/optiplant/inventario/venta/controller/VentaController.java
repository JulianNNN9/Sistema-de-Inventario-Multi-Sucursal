package com.optiplant.inventario.venta.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.venta.dto.SaleRequest;
import com.optiplant.inventario.venta.dto.SaleResponse;
import com.optiplant.inventario.venta.dto.SaleSummaryResponse;
import com.optiplant.inventario.venta.service.VentaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Ventas (RF-13..RF-16). El registro es de ADMIN / OPERADOR (Sección 4.2:
 * "Registrar ventas"); el comprobante y el histórico los consultan los tres
 * roles, acotados a la sucursal propia salvo ADMIN.
 */
@Tag(name = "Ventas", description = "Registro de ventas y comprobantes (Módulo 3).")
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','OPERADOR_INVENTARIO')")
    public SaleResponse crear(@Valid @RequestBody SaleRequest request) {
        return ventaService.crear(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public SaleResponse obtener(@PathVariable Long id) {
        return ventaService.obtener(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<SaleSummaryResponse> listar(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ventaService.listar(branchId, from, to, pageable);
    }
}
