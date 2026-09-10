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

export interface ProductoUnidad {
  id: number;
  productoId: number;
  nombreUnidad: string;
  factorConversion: number;
}

export interface ProductoUnidadInput {
  nombreUnidad: string;
  factorConversion: number;
}
