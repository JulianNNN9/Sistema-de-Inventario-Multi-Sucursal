import type { PageResponse } from '../../../shared/types/api';
import type {
  ApproveInput,
  DispatchInput,
  EstadoTransferencia,
  ReceiveInput,
  ResolveInput,
  Transfer,
  TransferEvent,
  TransferRequestInput,
} from '../types/transferencia';
import type { TransferSort } from '../../logistica/types/logistica';
import { apiClient } from '../../../shared/api/client';

interface ListParams {
  page?: number;
  size?: number;
  estado?: EstadoTransferencia;
  branchId?: number;
  sort?: TransferSort;
  soloActivas?: boolean;
}

export async function listTransfers(params: ListParams = {}): Promise<PageResponse<Transfer>> {
  const { data } = await apiClient.get<PageResponse<Transfer>>('/transfers', { params });
  return data;
}

export async function requestTransfer(body: TransferRequestInput): Promise<Transfer> {
  const { data } = await apiClient.post<Transfer>('/transfers', body);
  return data;
}

export async function approveTransfer(id: number, body: ApproveInput): Promise<Transfer> {
  const { data } = await apiClient.put<Transfer>(`/transfers/${id}/approve`, body);
  return data;
}

export async function dispatchTransfer(id: number, body: DispatchInput): Promise<Transfer> {
  const { data } = await apiClient.put<Transfer>(`/transfers/${id}/dispatch`, body);
  return data;
}

export async function receiveTransfer(id: number, body: ReceiveInput): Promise<Transfer> {
  const { data } = await apiClient.put<Transfer>(`/transfers/${id}/receive`, body);
  return data;
}

export async function resolveTransfer(id: number, body: ResolveInput): Promise<Transfer> {
  const { data } = await apiClient.put<Transfer>(`/transfers/${id}/resolve`, body);
  return data;
}

export async function getTransferEvents(
  id: number,
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<TransferEvent>> {
  const { data } = await apiClient.get<PageResponse<TransferEvent>>(`/transfers/${id}/events`, {
    params: { page: 0, size: 50, ...params },
  });
  return data;
}
