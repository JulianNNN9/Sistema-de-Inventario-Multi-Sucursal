export type Rol = 'ADMIN_GENERAL' | 'GERENTE_SUCURSAL' | 'OPERADOR_INVENTARIO';

/** Nombre en español natural de cada rol; nunca se muestra el valor técnico en la UI. */
export const ROL_LABEL: Record<Rol, string> = {
  ADMIN_GENERAL: 'Administrador general',
  GERENTE_SUCURSAL: 'Gerente de sucursal',
  OPERADOR_INVENTARIO: 'Operador de inventario',
};

export const ROL_OPTIONS: { value: Rol; label: string }[] = (
  Object.keys(ROL_LABEL) as Rol[]
).map((value) => ({ value, label: ROL_LABEL[value] }));

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
