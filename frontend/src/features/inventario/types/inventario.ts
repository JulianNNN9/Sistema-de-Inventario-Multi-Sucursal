export type TipoMovimiento = 'INGRESO' | 'RETIRO';

export type MotivoMovimiento =
  | 'COMPRA'
  | 'DEVOLUCION'
  | 'AJUSTE'
  | 'VENTA'
  | 'MERMA'
  | 'TRANSFERENCIA_SALIDA'
  | 'TRANSFERENCIA_ENTRADA';

export interface InventarioSucursal {
  productId: number;
  sku: string;
  productoNombre: string;
  branchId: number;
  sucursalNombre: string;
  cantidadActual: number;
  stockMinimo: number;
  costoPromedioPonderado: number;
  alertaStockBajo: boolean;
}

export interface MovimientoInput {
  productId: number;
  branchId: number;
  tipo: TipoMovimiento;
  motivo: MotivoMovimiento;
  cantidad: number;
}

export interface MovimientoResultado {
  movimientoId: number;
  productId: number;
  branchId: number;
  tipo: TipoMovimiento;
  motivo: MotivoMovimiento;
  cantidad: number;
  cantidadActual: number;
  stockMinimo: number;
  alertaStockBajo: boolean;
  fecha: string;
}

export interface MinStockInput {
  stockMinimo: number;
  branchId?: number;
}

/**
 * Motivos válidos por tipo en el endpoint manual (los TRANSFERENCIA_* los usa
 * solo el Módulo 4). COMPRA y VENTA se excluyen aquí a propósito: se generan
 * automáticamente desde Compras y Ventas respectivamente, no se registran a mano.
 */
export const MOTIVOS_POR_TIPO: Record<TipoMovimiento, MotivoMovimiento[]> = {
  INGRESO: ['DEVOLUCION', 'AJUSTE'],
  RETIRO: ['MERMA', 'AJUSTE'],
};
