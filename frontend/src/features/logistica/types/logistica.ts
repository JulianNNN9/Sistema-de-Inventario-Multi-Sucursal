/** Orden de listado de transferencias soportado por RF-23 (`GET /transfers?sort=`). */
export type TransferSort = 'priority' | 'cost' | 'time';

/**
 * Fila del reporte de cumplimiento logístico por ruta (RF-25, HU-13).
 * `ruta` = sucursal origen + transportista (proxy documentado en el roadmap).
 * `desviacionPromedioHoras`: positiva = tardanza promedio, negativa = adelanto
 * promedio, `null` si ninguna transferencia de la ruta ha llegado todavía.
 */
export interface ComplianceReportRow {
  sucursalOrigenId: number;
  sucursalOrigenNombre: string;
  transportista: string;
  cantidadTransferencias: number;
  desviacionPromedioHoras: number | null;
}
