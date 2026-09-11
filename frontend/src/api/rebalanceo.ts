import type { Transfer } from '../types/transferencia';
import type { RebalanceApproveInput, Sugerencia } from '../types/rebalanceo';
import { apiClient } from './client';

export async function listRebalanceSuggestions(): Promise<Sugerencia[]> {
  const { data } = await apiClient.get<Sugerencia[]>('/rebalance-suggestions');
  return data;
}

export async function approveRebalanceSuggestion(body: RebalanceApproveInput): Promise<Transfer> {
  const { data } = await apiClient.post<Transfer>('/rebalance-suggestions/approve', body);
  return data;
}
