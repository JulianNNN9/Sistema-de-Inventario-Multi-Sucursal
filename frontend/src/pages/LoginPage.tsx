import { useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Boxes, LogIn } from 'lucide-react';
import { Button, Input } from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import { useAuth } from '../hooks/useAuth';
import type { ApiError } from '../types/api';

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    const from = (location.state as { from?: string } | null)?.from ?? '/';
    return <Navigate to={from} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (err) {
      setError((err as ApiError).message ?? 'No se pudo iniciar sesión');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, ease: 'easeOut' }}
        className="w-full max-w-sm"
      >
        <div className="mb-6 flex flex-col items-center gap-3 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-sm">
            <Boxes className="h-6 w-6" aria-hidden />
          </div>
          <div>
            <h1 className="text-lg font-bold text-slate-900">Inventario Multi-Sucursal</h1>
            <p className="text-sm text-slate-500">OptiPlant Consultores</p>
          </div>
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm"
        >
          {error && <ErrorAlert message={error} />}

          <Input
            label="Email"
            hint="Correo con el que fuiste registrado en el sistema."
            type="email"
            required
            autoComplete="username"
            placeholder="admin@optiplant.local"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />

          <Input
            label="Contraseña"
            hint="La contraseña asignada por tu administrador."
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

          <Button type="submit" loading={submitting} className="w-full">
            {!submitting && <LogIn className="h-4 w-4" aria-hidden />}
            Iniciar sesión
          </Button>
        </form>
      </motion.div>
    </div>
  );
}
