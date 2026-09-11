import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ArrowLeftRight,
  Boxes,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  Receipt,
  ShieldCheck,
  ShoppingCart,
  Warehouse,
  type LucideIcon,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import type { Rol } from '../types/auth';
import { cn } from '../lib/cn';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  roles?: Rol[];
  end?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Panel', icon: LayoutDashboard, end: true },
  { to: '/products', label: 'Productos', icon: Package },
  { to: '/inventory', label: 'Inventario', icon: Warehouse },
  { to: '/purchases', label: 'Compras', icon: ShoppingCart },
  { to: '/sales', label: 'Ventas', icon: Receipt },
  { to: '/transfers', label: 'Transferencias', icon: ArrowLeftRight },
  { to: '/admin', label: 'Administración', icon: ShieldCheck, roles: ['ADMIN_GENERAL'] },
];

const ROL_LABEL: Record<Rol, string> = {
  ADMIN_GENERAL: 'Administrador general',
  GERENTE_SUCURSAL: 'Gerente de sucursal',
  OPERADOR_INVENTARIO: 'Operador de inventario',
};

function initials(name: string | null): string {
  if (!name) return '·';
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

export function Layout() {
  const { usuario, rol, logout } = useAuth();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const items = NAV_ITEMS.filter((item) => !item.roles || (rol !== null && item.roles.includes(rol)));

  const nav = (
    <div className="flex h-full flex-col gap-1 p-4">
      <div className="mb-4 flex items-center gap-2.5 px-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white">
          <Boxes className="h-5 w-5" aria-hidden />
        </div>
        <div className="leading-tight">
          <p className="text-sm font-semibold text-slate-900">OptiPlant</p>
          <p className="text-xs text-slate-500">Inventario Multi-Sucursal</p>
        </div>
      </div>
      <nav className="flex flex-col gap-1">
        {items.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-all duration-200 ease-in-out',
                isActive
                  ? 'bg-brand-50 text-brand-700'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
              )
            }
          >
            <Icon className="h-5 w-5 shrink-0" aria-hidden />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-200 bg-white md:block">
        {nav}
      </aside>

      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              className="fixed inset-0 z-40 bg-slate-900/30 backdrop-blur-sm md:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={() => setMobileOpen(false)}
            />
            <motion.aside
              className="fixed inset-y-0 left-0 z-50 w-64 border-r border-slate-200 bg-white md:hidden"
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'tween', duration: 0.2, ease: 'easeInOut' }}
            >
              {nav}
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      <div className="md:pl-64">
        <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-slate-200 bg-white/80 px-4 backdrop-blur-md sm:px-6">
          <button
            type="button"
            aria-label="Abrir menú de navegación"
            onClick={() => setMobileOpen(true)}
            className="rounded-lg p-2 text-slate-600 transition-colors hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 md:hidden"
          >
            <Menu className="h-5 w-5" aria-hidden />
          </button>

          <div className="ml-auto flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium text-slate-900">{usuario}</p>
              <p className="text-xs text-slate-500">{rol ? ROL_LABEL[rol] : ''}</p>
            </div>
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-xs font-semibold text-brand-700">
              {initials(usuario)}
            </div>
            <button
              type="button"
              onClick={logout}
              aria-label="Cerrar sesión"
              className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              <LogOut className="h-5 w-5" aria-hidden />
            </button>
          </div>
        </header>

        <main className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
