/** Un punto de la comparación de ventas (RF-26): `periodo` en formato `yyyy-MM`. */
export interface SalesComparisonPoint {
  periodo: string;
  totalVentas: number;
}

export interface InventoryRotationItem {
  productId: number;
  sku: string;
  nombre: string;
  cantidadRetirada: number;
}

/** RF-27: top 5 de mayor y de menor rotación (RETIRO, últimos 30 días). */
export interface InventoryRotationResponse {
  mayorRotacion: InventoryRotationItem[];
  menorRotacion: InventoryRotationItem[];
}

/** Únicos estados no terminales del ciclo de vida de una transferencia (RF-28). */
export type EstadoTransferenciaActivo = 'PENDIENTE' | 'EN_TRANSITO' | 'CON_FALTANTES' | 'REENVIO_SOLICITADO';

export interface ActiveTransfersCount {
  estado: EstadoTransferenciaActivo;
  cantidad: number;
}

/** RF-29: producto cuya existencia ya llegó (o bajó) del mínimo. */
export interface RestockAlert {
  productId: number;
  sku: string;
  productoNombre: string;
  sucursalId: number;
  sucursalNombre: string;
  cantidadActual: number;
  stockMinimo: number;
}

/** RF-30 (solo ADMIN_GENERAL): ventas del mes actual y rotación de 30 días, por sucursal. */
export interface BranchComparisonRow {
  sucursalId: number;
  sucursalNombre: string;
  ventasTotales: number;
  rotacionTotal: number;
}
