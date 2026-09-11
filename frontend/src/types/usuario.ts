import type { Rol } from './auth';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
  sucursalId: number | null;
  sucursalNombre: string | null;
}

export interface UsuarioInput {
  nombre: string;
  email: string;
  password: string;
  rol: Rol;
  sucursalId?: number;
}

export interface UsuarioUpdateInput {
  nombre: string;
  rol: Rol;
  sucursalId?: number;
  password?: string;
}
