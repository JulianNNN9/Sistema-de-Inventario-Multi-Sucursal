export type Rol = 'ADMIN_GENERAL' | 'GERENTE_SUCURSAL' | 'OPERADOR_INVENTARIO';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  rol: Rol;
  sucursalId: number | null;
  nombre: string;
}
