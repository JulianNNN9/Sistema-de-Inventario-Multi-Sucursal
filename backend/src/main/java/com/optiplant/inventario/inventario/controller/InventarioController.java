package com.optiplant.inventario.inventario.controller;

import com.optiplant.inventario.inventario.dto.InventarioResponse;
import com.optiplant.inventario.inventario.dto.MinStockRequest;
import com.optiplant.inventario.inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración de stock mínimo por producto y sucursal (RF-05). Restringido a
 * GERENTE_SUCURSAL y ADMIN_GENERAL (Sección 4.2).
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
public class InventarioController {

    private final InventarioService inventarioService;

    @PutMapping("/{productId}/min-stock")
    public InventarioResponse configurarStockMinimo(@PathVariable Long productId,
                                                    @Valid @RequestBody MinStockRequest request) {
        return inventarioService.configurarStockMinimo(productId, request);
    }
}
