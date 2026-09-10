export interface Proveedor {
  id: number;
  nombre: string;
  condiciones: string | null;
}

export interface ProveedorInput {
  nombre: string;
  condiciones?: string;
}

export type EstadoOrdenCompra = 'PENDIENTE' | 'RECIBIDA' | 'CANCELADA';

export interface PurchaseOrderLineInput {
  productId: number;
  cantidad: number;
  precioUnitario: number;
  descuento?: number;
}

export interface PurchaseOrderInput {
  supplierId: number;
  branchId?: number;
  plazoPago?: string;
  lineas: PurchaseOrderLineInput[];
}

export interface PurchaseOrderLine {
  productId: number;
  sku: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
}

export interface PurchaseOrder {
  id: number;
  supplierId: number;
  supplierNombre: string;
  branchId: number;
  sucursalNombre: string;
  fecha: string;
  estado: EstadoOrdenCompra;
  plazoPago: string | null;
  total: number;
  lineas: PurchaseOrderLine[];
}

export interface PurchaseOrderSummary {
  id: number;
  supplierId: number;
  supplierNombre: string;
  branchId: number;
  sucursalNombre: string;
  fecha: string;
  estado: EstadoOrdenCompra;
  total: number;
}
