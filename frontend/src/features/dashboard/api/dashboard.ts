import type {
  ActiveTransfersCount,
  BranchComparisonRow,
  InventoryRotationResponse,
  RestockAlert,
  SalesComparisonPoint,
} from '../types/dashboard';
import { apiClient } from '../../../shared/api/client';

interface BranchParams {
  branchId?: number;
}

export async function getSalesComparison(params: BranchParams = {}): Promise<SalesComparisonPoint[]> {
  const { data } = await apiClient.get<SalesComparisonPoint[]>('/dashboard/sales-comparison', { params });
  return data;
}

export async function getInventoryRotation(params: BranchParams = {}): Promise<InventoryRotationResponse> {
  const { data } = await apiClient.get<InventoryRotationResponse>('/dashboard/inventory-rotation', { params });
  return data;
}

export async function getActiveTransfers(params: BranchParams = {}): Promise<ActiveTransfersCount[]> {
  const { data } = await apiClient.get<ActiveTransfersCount[]>('/dashboard/active-transfers', { params });
  return data;
}

export async function getRestockAlerts(params: BranchParams = {}): Promise<RestockAlert[]> {
  const { data } = await apiClient.get<RestockAlert[]>('/dashboard/restock-alerts', { params });
  return data;
}

export async function getBranchComparison(): Promise<BranchComparisonRow[]> {
  const { data } = await apiClient.get<BranchComparisonRow[]>('/dashboard/branch-comparison');
  return data;
}
