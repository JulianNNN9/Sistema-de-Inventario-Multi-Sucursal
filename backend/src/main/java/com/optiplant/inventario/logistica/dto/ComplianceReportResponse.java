package com.optiplant.inventario.logistica.dto;

/**
 * Fila del reporte de cumplimiento logístico ({@code GET /logistics/compliance-report}, RF-25).
 * {@code ruta} = sucursal origen + transportista (proxy documentado en el roadmap).
 * {@code desviacionPromedioHoras}: positiva = tardanza promedio, negativa = adelanto
 * promedio, {@code null} si ninguna transferencia de la ruta ha llegado todavía.
 */
public record ComplianceReportResponse(
        Long sucursalOrigenId,
        String sucursalOrigenNombre,
        String transportista,
        long cantidadTransferencias,
        Double desviacionPromedioHoras
) {
}
