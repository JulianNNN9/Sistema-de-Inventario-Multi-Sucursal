import type { PageResponse } from '../types/api';
import type { PriceList, PriceListInput } from '../types/venta';
import { apiClient } from './client';

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
