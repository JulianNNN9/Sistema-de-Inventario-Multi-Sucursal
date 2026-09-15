export interface PriceListItem {
  productId: number;
  sku: string;
  productoNombre: string;
  precio: number;
}

export interface PriceList {
  id: number;
  nombre: string;
  branchId: number | null;
  sucursalNombre: string | null;
  items: PriceListItem[];
}

export interface PriceListItemInput {
  productId: number;
  precio: number;
}

export interface PriceListInput {
  nombre: string;
  branchId?: number;
  items: PriceListItemInput[];
}

export interface PriceListUpdateInput {
  nombre: string;
  items: PriceListItemInput[];
}

export interface SaleLineInput {
  productId: number;
  cantidad: number;
  priceListId?: number;
  precioUnitario?: number;
  descuento?: number;
}

export interface SaleInput {
  branchId?: number;
  lineas: SaleLineInput[];
}

export interface SaleLine {
  productId: number;
  sku: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
}

export interface Sale {
  id: number;
  branchId: number;
  sucursalNombre: string;
  usuarioId: number;
  usuarioNombre: string;
  fecha: string;
  total: number;
  lineas: SaleLine[];
}

export interface SaleSummary {
  id: number;
  branchId: number;
  sucursalNombre: string;
  usuarioNombre: string;
  fecha: string;
  total: number;
}
