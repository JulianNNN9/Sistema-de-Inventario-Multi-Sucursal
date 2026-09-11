import type { Urgencia } from './transferencia';

/** Sugerencia de rebalanceo entre sucursales (RF-31). No se persiste: se recalcula en cada GET. */
export interface Sugerencia {
  productId: number;
  sku: string;
  productoNombre: string;
  cantidadSugerida: number;
  sucursalOrigenId: number;
  sucursalOrigenNombre: string;
  sucursalDestinoId: number;
  sucursalDestinoNombre: string;
  urgencia: Urgencia;
}

/** Cuerpo de `POST /rebalance-suggestions/approve` (RF-33): misma forma que una {@link Sugerencia}, sin `urgencia`. */
export interface RebalanceApproveInput {
  productId: number;
  cantidadSugerida: number;
  sucursalOrigenId: number;
  sucursalDestinoId: number;
}
