import { useAuth } from '../hooks/useAuth';

export function DashboardPage() {
  const { usuario } = useAuth();

  return (
    <div className="space-y-2">
      <h1 className="text-xl font-semibold text-gray-900">Panel</h1>
      <p className="text-sm text-gray-600">
        Bienvenido, {usuario}. El tablero con indicadores (ventas, rotación,
        transferencias activas, reabastecimiento) se habilita en el Módulo 6.
      </p>
    </div>
  );
}
