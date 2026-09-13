import type { ComponentType, ReactElement } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { Rol } from '../types/auth';

interface RoleGuardProps {
  allow: Rol[];
  children?: ReactElement;
}

/**
 * Restringe el acceso a vistas según el rol autenticado (Sección 4.2 / Sección 6).
 * Como elemento de ruta envuelve un {@link Outlet}; con `children` envuelve un
 * componente concreto.
 */
export function RoleGuard({ allow, children }: RoleGuardProps) {
  const { rol } = useAuth();

  if (!rol || !allow.includes(rol)) {
    return <Navigate to="/403" replace />;
  }
  return children ?? <Outlet />;
}

/** Forma HOC (Sección 6): envuelve un componente exigiendo uno de los roles. */
export function withRoleGuard<P extends object>(
  Component: ComponentType<P>,
  allow: Rol[],
): ComponentType<P> {
  return function Guarded(props: P) {
    return (
      <RoleGuard allow={allow}>
        <Component {...props} />
      </RoleGuard>
    );
  };
}
