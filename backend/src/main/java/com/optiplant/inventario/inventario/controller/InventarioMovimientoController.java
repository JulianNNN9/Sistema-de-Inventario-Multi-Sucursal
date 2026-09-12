package com.optiplant.inventario.inventario.controller;

import com.optiplant.inventario.inventario.dto.MovimientoRequest;
import com.optiplant.inventario.inventario.dto.MovimientoResponse;
import com.optiplant.inventario.inventario.service.InventarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro de ingresos y retiros de inventario (RF-03, RF-04, RF-07). El alcance
 * por sucursal se valida en el Service (regla transversal RF-36). Los tres roles
 * pueden registrar movimientos de su propia sucursal.
 */
@Tag(name = "Movimientos de inventario", description = "Registro de ingresos y retiros de stock (Módulo 1).")
@RestController
@RequestMapping("/api/v1/inventory-movements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
public class InventarioMovimientoController {

    private final InventarioService inventarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(@Valid @RequestBody MovimientoRequest request) {
        return inventarioService.registrarMovimiento(request);
    }
}
