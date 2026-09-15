import type { PageResponse } from '../../../shared/types/api';
import type {
  InventarioSucursal,
  MinStockInput,
  MovimientoInput,
  MovimientoResultado,
} from '../types/inventario';
import { apiClient } from '../../../shared/api/client';

export async function getBranchInventory(
  branchId: number,
  params: { page?: number; size?: number; search?: string; soloBajoMinimo?: boolean } = {},
): Promise<PageResponse<InventarioSucursal>> {
  const { data } = await apiClient.get<PageResponse<InventarioSucursal>>(
    `/branches/${branchId}/inventory`,
    { params },
  );
  return data;
}

export async function registerMovement(body: MovimientoInput): Promise<MovimientoResultado> {
  const { data } = await apiClient.post<MovimientoResultado>('/inventory-movements', body);
  return data;
}

export async function setMinStock(
  productId: number,
  body: MinStockInput,
): Promise<InventarioSucursal> {
  const { data } = await apiClient.put<InventarioSucursal>(
    `/inventory/${productId}/min-stock`,
    body,
  );
  return data;
}
