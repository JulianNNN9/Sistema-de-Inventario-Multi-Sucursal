import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { AdminPage } from './pages/AdminPage';
import { ComprasPage } from './pages/ComprasPage';
import { DashboardPage } from './pages/DashboardPage';
import { InventarioPage } from './pages/InventarioPage';
import { LoginPage } from './pages/LoginPage';
import { LogisticaPage } from './pages/LogisticaPage';
import { NotAuthorizedPage } from './pages/NotAuthorizedPage';
import { ProductosPage } from './pages/ProductosPage';
import { TransferenciasPage } from './pages/TransferenciasPage';
import { VentasPage } from './pages/VentasPage';
import { PrivateRoute } from './routes/PrivateRoute';
import { RoleGuard } from './routes/RoleGuard';

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
                <Route element={<RoleGuard allow={['ADMIN_GENERAL']} />}>
                  <Route path="logistics" element={<LogisticaPage />} />
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
