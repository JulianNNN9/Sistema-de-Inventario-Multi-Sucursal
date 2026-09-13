import type { LoginRequest, LoginResponse } from '../types/auth';
import { apiClient } from '../../../shared/api/client';

export async function login(body: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', body);
  return data;
}
