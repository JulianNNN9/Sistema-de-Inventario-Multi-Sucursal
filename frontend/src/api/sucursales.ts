import type { PageResponse } from '../types/api';
import type { Sucursal } from '../types/sucursal';
import { apiClient } from './client';

export async function listBranches(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<Sucursal>> {
  const { data } = await apiClient.get<PageResponse<Sucursal>>('/branches', {
    params: { page: 0, size: 100, ...params },
  });
  return data;
}
