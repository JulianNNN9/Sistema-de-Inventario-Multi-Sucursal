import { Link } from 'react-router-dom';

export function NotAuthorizedPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2 bg-gray-50 px-4 text-center">
      <h1 className="text-2xl font-semibold text-gray-900">403 · Sin permisos</h1>
      <p className="text-sm text-gray-600">Tu rol no tiene acceso a esta sección.</p>
      <Link to="/" className="text-sm text-brand hover:underline">
        Volver al panel
      </Link>
    </div>
  );
}
