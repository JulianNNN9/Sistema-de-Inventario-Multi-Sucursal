import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { Rol } from '../types/auth';

interface NavItem {
  to: string;
  label: string;
  roles?: Rol[];
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Panel' },
  { to: '/admin', label: 'Administración', roles: ['ADMIN_GENERAL'] },
];

const ROL_LABEL: Record<Rol, string> = {
  ADMIN_GENERAL: 'Administrador general',
  GERENTE_SUCURSAL: 'Gerente de sucursal',
  OPERADOR_INVENTARIO: 'Operador de inventario',
};

/** Shell de la aplicación: navegación condicionada por rol + área de contenido. */
export function Layout() {
  const { usuario, rol, logout } = useAuth();
  const items = NAV_ITEMS.filter((item) => !item.roles || (rol !== null && item.roles.includes(rol)));

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
          <div className="flex items-center gap-6">
            <span className="font-semibold text-brand">OptiPlant</span>
            <nav className="flex gap-4">
              {items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    `text-sm ${
                      isActive ? 'font-medium text-brand' : 'text-gray-600 hover:text-gray-900'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-gray-500">
              {usuario}
              {rol ? ` · ${ROL_LABEL[rol]}` : ''}
            </span>
            <button
              type="button"
              onClick={logout}
              className="rounded border border-gray-300 px-3 py-1 text-gray-700 hover:bg-gray-100"
            >
              Salir
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
