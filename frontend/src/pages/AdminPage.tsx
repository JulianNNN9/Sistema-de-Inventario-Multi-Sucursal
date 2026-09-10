export function AdminPage() {
  return (
    <div className="space-y-2">
      <h1 className="text-xl font-semibold text-gray-900">Administración</h1>
      <p className="text-sm text-gray-600">
        Gestión de usuarios y sucursales. Ruta protegida por rol
        (<code>ADMIN_GENERAL</code>). Se conecta a <code>/api/v1/users</code> y{' '}
        <code>/api/v1/branches</code> en fases posteriores.
      </p>
    </div>
  );
}
