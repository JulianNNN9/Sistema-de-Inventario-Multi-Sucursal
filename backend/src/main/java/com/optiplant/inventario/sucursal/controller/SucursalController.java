package com.optiplant.inventario.sucursal.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.sucursal.dto.SucursalRequest;
import com.optiplant.inventario.sucursal.dto.SucursalResponse;
import com.optiplant.inventario.sucursal.service.SucursalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración de sucursales. El alta ({@code POST}) es exclusiva de
 * {@code ADMIN_GENERAL} (Sección 4.2); el listado ({@code GET}) es de solo
 * lectura y lo necesitan los tres roles: para consultar el inventario de otras
 * sucursales (RF-02, HU-02) y para elegir origen/destino en transferencias
 * (Módulo 4).
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalService sucursalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public SucursalResponse crear(@Valid @RequestBody SucursalRequest request) {
        return sucursalService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<SucursalResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return sucursalService.listar(pageable);
    }
}
