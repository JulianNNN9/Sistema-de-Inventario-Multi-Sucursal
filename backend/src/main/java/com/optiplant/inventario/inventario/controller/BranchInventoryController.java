package com.optiplant.inventario.inventario.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.inventario.dto.InventarioResponse;
import com.optiplant.inventario.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta del inventario de cualquier sucursal en modo solo lectura (RF-02).
 * Disponible para los tres roles (Sección 4.2). Endpoint separado de
 * {@code SucursalController} para no heredar su restricción a ADMIN_GENERAL.
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
public class BranchInventoryController {

    private final InventarioService inventarioService;

    @GetMapping("/{branchId}/inventory")
    public PageResponse<InventarioResponse> inventarioDeSucursal(
            @PathVariable Long branchId, @PageableDefault(size = 20) Pageable pageable) {
        return inventarioService.listarInventarioSucursal(branchId, pageable);
    }
}
