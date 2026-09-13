import type { Rol } from '../types/auth';

const STORAGE_KEY = 'optiplant.auth';

export interface StoredSession {
  token: string;
  rol: Rol;
  sucursalId: number | null;
  nombre: string;
}

export function getSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}

export function setSession(session: StoredSession): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY);
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

/**
 * Comprueba la expiración del JWT sin librerías: decodifica el payload y compara
 * `exp`. No hay refresh token (Sección 4.1): un token expirado obliga a re-login.
 */
export function isTokenExpired(token: string): boolean {
  try {
    const [, payload] = token.split('.');
    const claims = JSON.parse(atob(payload ?? '')) as { exp?: number };
    return typeof claims.exp === 'number' && claims.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}
