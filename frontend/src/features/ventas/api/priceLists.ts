import type { PageResponse } from '../../../shared/types/api';
import type { PriceList, PriceListInput, PriceListUpdateInput } from '../types/venta';
import { apiClient } from '../../../shared/api/client';

export async function listPriceLists(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<PriceList>> {
  const { data } = await apiClient.get<PageResponse<PriceList>>('/price-lists', {
    params: { page: 0, size: 100, ...params },
  });
  return data;
}

export async function createPriceList(body: PriceListInput): Promise<PriceList> {
  const { data } = await apiClient.post<PriceList>('/price-lists', body);
  return data;
}

export async function updatePriceList(id: number, body: PriceListUpdateInput): Promise<PriceList> {
  const { data } = await apiClient.put<PriceList>(`/price-lists/${id}`, body);
  return data;
}
