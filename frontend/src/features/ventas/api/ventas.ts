import type { PageResponse } from '../../../shared/types/api';
import type { Sale, SaleInput, SaleSummary } from '../types/venta';
import { apiClient } from '../../../shared/api/client';

interface ListParams {
  page?: number;
  size?: number;
  branchId?: number;
  /** yyyy-MM-dd */
  from?: string;
  /** yyyy-MM-dd */
  to?: string;
}

export async function listSales(params: ListParams = {}): Promise<PageResponse<SaleSummary>> {
  const { data } = await apiClient.get<PageResponse<SaleSummary>>('/sales', { params });
  return data;
}

export async function getSale(id: number): Promise<Sale> {
  const { data } = await apiClient.get<Sale>(`/sales/${id}`);
  return data;
}

export async function createSale(body: SaleInput): Promise<Sale> {
  const { data } = await apiClient.post<Sale>('/sales', body);
  return data;
}
