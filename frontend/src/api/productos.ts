import type { PageResponse } from '../types/api';
import type { Producto, ProductoInput, ProductoUpdateInput } from '../types/producto';
import { apiClient } from './client';

interface ListParams {
  page?: number;
  size?: number;
  branchId?: number;
}

export async function listProducts(params: ListParams = {}): Promise<PageResponse<Producto>> {
  const { data } = await apiClient.get<PageResponse<Producto>>('/products', { params });
  return data;
}

export async function getProduct(id: number): Promise<Producto> {
  const { data } = await apiClient.get<Producto>(`/products/${id}`);
  return data;
}

export async function createProduct(body: ProductoInput): Promise<Producto> {
  const { data } = await apiClient.post<Producto>('/products', body);
  return data;
}

export async function updateProduct(id: number, body: ProductoUpdateInput): Promise<Producto> {
  const { data } = await apiClient.put<Producto>(`/products/${id}`, body);
  return data;
}

export async function deleteProduct(id: number): Promise<void> {
  await apiClient.delete(`/products/${id}`);
}
