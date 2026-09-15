import type { PageResponse } from '../../../shared/types/api';
import type { PurchaseOrder, PurchaseOrderInput, PurchaseOrderSummary } from '../types/compra';
import { apiClient } from '../../../shared/api/client';

interface ListParams {
  page?: number;
  size?: number;
  supplierId?: number;
  productId?: number;
  branchId?: number;
  soloActivas?: boolean;
}

export async function listPurchaseOrders(
  params: ListParams = {},
): Promise<PageResponse<PurchaseOrderSummary>> {
  const { data } = await apiClient.get<PageResponse<PurchaseOrderSummary>>('/purchase-orders', {
    params,
  });
  return data;
}

/** Worklist de OPERADOR_INVENTARIO: solo las pendientes de su propia sucursal. */
export async function listPendingPurchaseOrders(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<PurchaseOrderSummary>> {
  const { data } = await apiClient.get<PageResponse<PurchaseOrderSummary>>('/purchase-orders/pending', {
    params,
  });
  return data;
}

export async function getPurchaseOrder(id: number): Promise<PurchaseOrder> {
  const { data } = await apiClient.get<PurchaseOrder>(`/purchase-orders/${id}`);
  return data;
}

export async function createPurchaseOrder(body: PurchaseOrderInput): Promise<PurchaseOrder> {
  const { data } = await apiClient.post<PurchaseOrder>('/purchase-orders', body);
  return data;
}

export async function confirmReceipt(id: number): Promise<PurchaseOrder> {
  const { data } = await apiClient.post<PurchaseOrder>(`/purchase-orders/${id}/confirm-receipt`);
  return data;
}

export async function cancelPurchaseOrder(id: number): Promise<PurchaseOrder> {
  const { data } = await apiClient.post<PurchaseOrder>(`/purchase-orders/${id}/cancel`);
  return data;
}
