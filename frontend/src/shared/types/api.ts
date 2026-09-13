/** Formato de error estándar del backend (Sección 5 del roadmap). */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

/** Envoltura de paginación estándar del backend (Sección 5). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
