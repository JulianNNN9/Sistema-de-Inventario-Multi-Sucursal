import type { PageResponse } from '../types/api';
import type { Usuario, UsuarioInput, UsuarioUpdateInput } from '../types/usuario';
import { apiClient } from './client';

export async function listUsers(
  params: { page?: number; size?: number } = {},
): Promise<PageResponse<Usuario>> {
  const { data } = await apiClient.get<PageResponse<Usuario>>('/users', {
    params: { page: 0, size: 100, ...params },
  });
  return data;
}

export async function createUser(body: UsuarioInput): Promise<Usuario> {
  const { data } = await apiClient.post<Usuario>('/users', body);
  return data;
}

export async function updateUser(id: number, body: UsuarioUpdateInput): Promise<Usuario> {
  const { data } = await apiClient.put<Usuario>(`/users/${id}`, body);
  return data;
}
