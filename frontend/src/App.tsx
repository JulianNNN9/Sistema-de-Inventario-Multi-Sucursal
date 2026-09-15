import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './shared/components/Layout';
import { AuthProvider } from './features/auth/context/AuthContext';
import { ToastProvider } from './shared/context/ToastContext';
import { Spinner } from './shared/components/ui';
import { LoginPage } from './features/auth/pages/LoginPage';
import { NotAuthorizedPage } from './features/auth/pages/NotAuthorizedPage';
import { PrivateRoute } from './features/auth/routes/PrivateRoute';
import { RoleGuard } from './features/auth/routes/RoleGuard';

// Cada página se carga en su propio archivo bajo demanda (code splitting por
// ruta), en vez de empaquetar todo el catálogo de páginas en el bundle
// inicial: reduce lo que el navegador descarga y parsea al entrar a la app.
const DashboardPage = lazy(() => import('./features/dashboard/pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const ProductosPage = lazy(() => import('./features/productos/pages/ProductosPage').then((m) => ({ default: m.ProductosPage })));
const InventarioPage = lazy(() => import('./features/inventario/pages/InventarioPage').then((m) => ({ default: m.InventarioPage })));
const ComprasPage = lazy(() => import('./features/compras/pages/ComprasPage').then((m) => ({ default: m.ComprasPage })));
const VentasPage = lazy(() => import('./features/ventas/pages/VentasPage').then((m) => ({ default: m.VentasPage })));
const TransferenciasPage = lazy(() => import('./features/transferencias/pages/TransferenciasPage').then((m) => ({ default: m.TransferenciasPage })));
const LogisticaPage = lazy(() => import('./features/logistica/pages/LogisticaPage').then((m) => ({ default: m.LogisticaPage })));
const AdminPage = lazy(() => import('./features/admin/pages/AdminPage').then((m) => ({ default: m.AdminPage })));

function PageFallback() {
  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <Spinner className="h-8 w-8" />
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <Suspense fallback={<PageFallback />}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/403" element={<NotAuthorizedPage />} />

              <Route element={<PrivateRoute />}>
                <Route element={<Layout />}>
                  <Route index element={<DashboardPage />} />
                  <Route path="products" element={<ProductosPage />} />
                  <Route path="inventory" element={<InventarioPage />} />
                  <Route path="purchases" element={<ComprasPage />} />
                  <Route path="sales" element={<VentasPage />} />
                  <Route path="transfers" element={<TransferenciasPage />} />
                  <Route element={<RoleGuard allow={['ADMIN_GENERAL', 'GERENTE_SUCURSAL']} />}>
                    <Route path="logistics" element={<LogisticaPage />} />
                  </Route>
                  <Route element={<RoleGuard allow={['ADMIN_GENERAL']} />}>
                    <Route path="admin" element={<AdminPage />} />
                  </Route>
                </Route>
              </Route>

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
