package com.optiplant.inventario.dashboard.controller;

import com.optiplant.inventario.dashboard.dto.ActiveTransfersCount;
import com.optiplant.inventario.dashboard.dto.BranchComparisonRow;
import com.optiplant.inventario.dashboard.dto.InventoryRotationResponse;
import com.optiplant.inventario.dashboard.dto.RestockAlert;
import com.optiplant.inventario.dashboard.dto.SalesComparisonPoint;
import com.optiplant.inventario.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Indicadores operativos (RF-26..RF-30). Todos GET, sin cuerpo, sin paginar
 * (reportes acotados: 4 meses, 5+5 productos, un puñado de estados o
 * sucursales — mismo criterio que el reporte de logística del Módulo 5).
 * {@code branchId} lo respeta solo ADMIN_GENERAL; el resto de roles siempre
 * ve su propia sucursal (Sección 4.2).
 */
@Tag(name = "Dashboard", description = "Indicadores operativos: ventas, rotación, alertas y comparativas (Módulo 6).")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/sales-comparison")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public List<SalesComparisonPoint> salesComparison(
            @RequestParam(name = "branchId", required = false) Long branchId) {
        return dashboardService.salesComparison(branchId);
    }

    @GetMapping("/inventory-rotation")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public InventoryRotationResponse inventoryRotation(
            @RequestParam(name = "branchId", required = false) Long branchId) {
        return dashboardService.inventoryRotation(branchId);
    }

    @GetMapping("/active-transfers")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public List<ActiveTransfersCount> activeTransfers(
            @RequestParam(name = "branchId", required = false) Long branchId) {
        return dashboardService.activeTransfers(branchId);
    }

    @GetMapping("/restock-alerts")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','GERENTE_SUCURSAL','OPERADOR_INVENTARIO')")
    public List<RestockAlert> restockAlerts(
            @RequestParam(name = "branchId", required = false) Long branchId) {
        return dashboardService.restockAlerts(branchId);
    }

    @GetMapping("/branch-comparison")
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public List<BranchComparisonRow> branchComparison() {
        return dashboardService.branchComparison();
    }
}
