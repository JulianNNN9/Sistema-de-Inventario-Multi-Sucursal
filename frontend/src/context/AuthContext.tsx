import { createContext, useCallback, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { clearSession, getSession, isTokenExpired, setSession } from '../api/session';
import type { Rol } from '../types/auth';

export interface AuthContextValue {
  token: string | null;
  usuario: string | null;
  rol: Rol | null;
  sucursalId: number | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

interface AuthState {
  token: string | null;
  usuario: string | null;
  rol: Rol | null;
  sucursalId: number | null;
}

const EMPTY_STATE: AuthState = {
  token: null,
  usuario: null,
  rol: null,
  sucursalId: null,
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readInitialState(): AuthState {
  const session = getSession();
  if (!session || isTokenExpired(session.token)) {
    clearSession();
    return EMPTY_STATE;
  }
  return {
    token: session.token,
    usuario: session.nombre,
    rol: session.rol,
    sucursalId: session.sucursalId,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(readInitialState);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password });
    setSession({
      token: res.token,
      rol: res.rol,
      sucursalId: res.sucursalId,
      nombre: res.nombre,
    });
    setState({
      token: res.token,
      usuario: res.nombre,
      rol: res.rol,
      sucursalId: res.sucursalId,
    });
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setState(EMPTY_STATE);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, isAuthenticated: state.token !== null, login, logout }),
    [state, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
