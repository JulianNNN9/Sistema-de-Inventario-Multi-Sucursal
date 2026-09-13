import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './shared/components/Layout';
import { AuthProvider } from './features/auth/context/AuthContext';
import { ToastProvider } from './shared/context/ToastContext';
import { AdminPage } from './features/admin/pages/AdminPage';
import { ComprasPage } from './features/compras/pages/ComprasPage';
import { DashboardPage } from './features/dashboard/pages/DashboardPage';
import { InventarioPage } from './features/inventario/pages/InventarioPage';
import { LoginPage } from './features/auth/pages/LoginPage';
import { LogisticaPage } from './features/logistica/pages/LogisticaPage';
import { NotAuthorizedPage } from './features/auth/pages/NotAuthorizedPage';
import { ProductosPage } from './features/productos/pages/ProductosPage';
import { TransferenciasPage } from './features/transferencias/pages/TransferenciasPage';
import { VentasPage } from './features/ventas/pages/VentasPage';
import { PrivateRoute } from './features/auth/routes/PrivateRoute';
import { RoleGuard } from './features/auth/routes/RoleGuard';

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
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
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
