package com.optiplant.inventario.compra.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.compra.dto.PurchaseOrderRequest;
import com.optiplant.inventario.compra.dto.PurchaseOrderResponse;
import com.optiplant.inventario.compra.dto.PurchaseOrderSummaryResponse;
import com.optiplant.inventario.compra.service.CompraService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

/**
 * Órdenes de compra (RF-08..RF-12). Crear la orden es de ADMIN_GENERAL /
 * GERENTE_SUCURSAL; confirmar la recepción de mercancía la hacen los tres
 * roles (incluye a OPERADOR_INVENTARIO, quien ejecuta la recepción física);
 * el histórico (listado/detalle, con filtros y precios) es de ADMIN_GENERAL /
 * GERENTE_SUCURSAL, acotado a la sucursal propia salvo ADMIN. OPERADOR_INVENTARIO
 * no ve el histórico, pero sí su propia worklist de "pendientes" (ver
 * {@code /pending}) para saber qué confirmar.
 */
@Tag(name = "Compras", description = "Órdenes de compra y confirmación de recepción (Módulo 2).")
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public PurchaseOrderResponse crear(@Valid @RequestBody PurchaseOrderRequest request) {
        return compraService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public PageResponse<PurchaseOrderSummaryResponse> listar(
            @RequestParam(name = "supplierId", required = false) Long supplierId,
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        return compraService.listar(supplierId, productId, branchId, pageable);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public PageResponse<PurchaseOrderSummaryResponse> pendientes(
            @PageableDefault(size = 20) Pageable pageable) {
        return compraService.listarPendientesPropios(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public PurchaseOrderResponse obtener(@PathVariable Long id) {
        return compraService.obtener(id);
    }

    @PostMapping("/{id}/confirm-receipt")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PurchaseOrderResponse confirmarRecepcion(@PathVariable Long id) {
        return compraService.confirmarRecepcion(id);
    }
}
