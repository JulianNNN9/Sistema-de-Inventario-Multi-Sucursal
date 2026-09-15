package com.optiplant.inventario.venta.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.venta.dto.PriceListRequest;
import com.optiplant.inventario.venta.dto.PriceListResponse;
import com.optiplant.inventario.venta.dto.PriceListUpdateRequest;
import com.optiplant.inventario.venta.service.ListaPrecioService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listas de precios (RF-15). Es una decisión de política comercial de la
 * sucursal, no una tarea de ejecución día a día: el alta la hace ADMIN_GENERAL
 * o GERENTE_SUCURSAL; OPERADOR_INVENTARIO solo las usa al registrar una venta
 * (ver RESPONSABILIDADES_ROLES.md, Sección 4). La consulta la hacen los tres roles.
 */
@Tag(name = "Listas de precios", description = "Listas de precios globales o por sucursal (Módulo 3).")
@RestController
@RequestMapping("/api/v1/price-lists")
@RequiredArgsConstructor
public class ListaPrecioController {

    private final ListaPrecioService listaPrecioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public PriceListResponse crear(@Valid @RequestBody PriceListRequest request) {
        return listaPrecioService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<PriceListResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return listaPrecioService.listar(pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public PriceListResponse actualizar(@PathVariable Long id, @Valid @RequestBody PriceListUpdateRequest request) {
        return listaPrecioService.actualizar(id, request);
    }
}
