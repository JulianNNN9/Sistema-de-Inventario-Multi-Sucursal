package com.optiplant.inventario.transferencia.controller;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.transferencia.dto.TransportistaRequest;
import com.optiplant.inventario.transferencia.dto.TransportistaResponse;
import com.optiplant.inventario.transferencia.service.TransportistaService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Catálogo de transportistas (Módulo 4). Mismos roles que pueden despachar una
 * transferencia, ya que es a quienes les hace falta el catálogo en ese momento.
 */
@Tag(name = "Transportistas", description = "Catálogo de transportistas usados al despachar una transferencia (Módulo 4).")
@RestController
@RequestMapping("/api/v1/carriers")
@RequiredArgsConstructor
public class TransportistaController {

    private final TransportistaService transportistaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public TransportistaResponse crear(@Valid @RequestBody TransportistaRequest request) {
        return transportistaService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public PageResponse<TransportistaResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return transportistaService.listar(pageable);
    }
}
