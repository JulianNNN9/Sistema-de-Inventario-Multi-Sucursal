package com.optiplant.inventario.logistica.controller;

import com.optiplant.inventario.logistica.dto.ComplianceReportResponse;
import com.optiplant.inventario.logistica.service.LogisticaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reporte de cumplimiento logístico por ruta (RF-25). No paginado (agregación
 * acotada por nº de sucursales × transportistas, igual que el Dashboard del
 * Módulo 6). ADMIN_GENERAL ve todas las sucursales; GERENTE_SUCURSAL solo ve
 * el reporte de su propia sucursal (el {@code branchId} que envíe se ignora y
 * se fuerza en el Service — ver RESPONSABILIDADES_ROLES.md, Sección 5).
 */
@Tag(name = "Logística", description = "Reporte de cumplimiento de tiempos de entrega por ruta (Módulo 5).")
@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
public class LogisticaController {

    private final LogisticaService logisticaService;

    @GetMapping("/compliance-report")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL')")
    public List<ComplianceReportResponse> complianceReport(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "route", required = false) String route) {
        return logisticaService.complianceReport(branchId, route);
    }
}
