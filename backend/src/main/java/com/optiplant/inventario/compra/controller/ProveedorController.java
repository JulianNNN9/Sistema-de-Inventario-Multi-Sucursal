package com.optiplant.inventario.compra.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.compra.dto.ProveedorRequest;
import com.optiplant.inventario.compra.dto.ProveedorResponse;
import com.optiplant.inventario.compra.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proveedores (RF-08). El alta y la edición forman parte del flujo de compras
 * (ADMIN / OPERADOR); el listado lo consultan los tres roles, entre otros para
 * el histórico de compras por proveedor (HU-06).
 */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','OPERADOR_INVENTARIO')")
    public ProveedorResponse crear(@Valid @RequestBody ProveedorRequest request) {
        return proveedorService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','OPERADOR_INVENTARIO')")
    public ProveedorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return proveedorService.actualizar(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<ProveedorResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return proveedorService.listar(pageable);
    }
}
