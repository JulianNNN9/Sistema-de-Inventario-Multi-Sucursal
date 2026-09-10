import type { PageResponse } from '../types/api';
import type { Proveedor, ProveedorInput } from '../types/compra';
import { apiClient } from './client';

export async function listSuppliers(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<Proveedor>> {
  const { data } = await apiClient.get<PageResponse<Proveedor>>('/suppliers', {
    params: { page: 0, size: 100, ...params },
  });
  return data;
}

export async function createSupplier(body: ProveedorInput): Promise<Proveedor> {
  const { data } = await apiClient.post<Proveedor>('/suppliers', body);
  return data;
}
