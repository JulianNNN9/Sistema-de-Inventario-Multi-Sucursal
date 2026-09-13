export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  unidadMedidaBase: string;
}

export interface ProductoInput {
  sku: string;
  nombre: string;
  unidadMedidaBase: string;
}

export interface ProductoUpdateInput {
  nombre: string;
  unidadMedidaBase: string;
}
