import type { PageResponse } from '../../../shared/types/api';
import type { Transportista, TransportistaInput } from '../types/transferencia';
import { apiClient } from '../../../shared/api/client';

export async function listCarriers(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<Transportista>> {
  const { data } = await apiClient.get<PageResponse<Transportista>>('/carriers', {
    params: { page: 0, size: 200, ...params },
  });
  return data;
}

export async function createCarrier(body: TransportistaInput): Promise<Transportista> {
  const { data } = await apiClient.post<Transportista>('/carriers', body);
  return data;
}
