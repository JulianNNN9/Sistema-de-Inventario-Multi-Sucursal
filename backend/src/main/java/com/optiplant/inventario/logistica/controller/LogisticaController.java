package com.optiplant.inventario.logistica.controller;

import com.optiplant.inventario.logistica.dto.ComplianceReportResponse;
import com.optiplant.inventario.logistica.service.LogisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reporte de cumplimiento logístico por ruta (RF-25, HU-13: "Como administrador
 * general, quiero un reporte de cumplimiento logístico por ruta"). No paginado
 * (agregación acotada por nº de sucursales × transportistas, igual que el
 * Dashboard del Módulo 6).
 */
@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
public class LogisticaController {

    private final LogisticaService logisticaService;

    @GetMapping("/compliance-report")
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public List<ComplianceReportResponse> complianceReport(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "route", required = false) String route) {
        return logisticaService.complianceReport(branchId, route);
    }
}
