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
 * Catálogo de productos (RF-01). Accesible a los tres roles ("CRUD
 * productos/inventario de su propia sucursal", Sección 4.2); el acotado por
 * sucursal se resuelve en el Service.
 */
@Tag(name = "Productos", description = "Catálogo de productos (Módulo 1).")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public PageResponse<ProductoResponse> listar(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        return productoService.listar(branchId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoUpdateRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
