import axios, { type AxiosError } from 'axios';
import type { ApiError } from '../types/api';
import { clearSession, getToken } from './session';

/**
 * Instancia única de Axios (patrón Adapter/Proxy, Sección 6). Aísla al resto de
 * la app de los detalles del transporte HTTP: agrega el JWT en cada petición y
 * normaliza cualquier error al formato de la Sección 5 ({@link ApiError}).
 */
export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      clearSession();
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(normalizeError(error));
  },
);

function normalizeError(error: AxiosError<ApiError>): ApiError {
  const data = error.response?.data;
  if (data && typeof data === 'object' && typeof data.message === 'string') {
    return data;
  }
  return {
    timestamp: new Date().toISOString(),
    status: error.response?.status ?? 0,
    error: error.response?.statusText || 'Error de red',
    message: error.message || 'No se pudo conectar con el servidor',
    path: error.config?.url ?? '',
  };
}
