package com.optiplant.inventario.producto.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.producto.dto.ProductoRequest;
import com.optiplant.inventario.producto.dto.ProductoResponse;
import com.optiplant.inventario.producto.dto.ProductoUpdateRequest;
import com.optiplant.inventario.producto.service.ProductoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Catálogo de productos (RF-01). Es un dato maestro único para toda la red (sin
 * {@code sucursal_id}): los tres roles lo consultan, pero solo ADMIN_GENERAL y
 * GERENTE_SUCURSAL lo mantienen (crear/editar/eliminar). OPERADOR_INVENTARIO es
 * de solo lectura sobre el catálogo.
 */
@Tag(name = "Productos", description = "Catálogo de productos (Módulo 1).")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<ProductoResponse> listar(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return productoService.listar(branchId, search, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoUpdateRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
