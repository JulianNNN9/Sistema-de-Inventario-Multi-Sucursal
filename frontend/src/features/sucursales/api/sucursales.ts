import type { PageResponse } from '../../../shared/types/api';
import type { Sucursal } from '../types/sucursal';
import { apiClient } from '../../../shared/api/client';

export interface SucursalInput {
  nombre: string;
  ciudad?: string;
}

export async function listBranches(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<Sucursal>> {
  const { data } = await apiClient.get<PageResponse<Sucursal>>('/branches', {
    params: { page: 0, size: 100, ...params },
  });
  return data;
}

export async function createBranch(body: SucursalInput): Promise<Sucursal> {
  const { data } = await apiClient.post<Sucursal>('/branches', body);
  return data;
}
