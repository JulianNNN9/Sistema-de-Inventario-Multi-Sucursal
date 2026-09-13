import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ShieldAlert } from 'lucide-react';
import { Button } from '../../../shared/components/ui';

export function NotAuthorizedPage() {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.2, ease: 'easeOut' }}
        className="flex max-w-sm flex-col items-center gap-3 text-center"
      >
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-600">
          <ShieldAlert className="h-7 w-7" aria-hidden />
        </div>
        <h1 className="text-xl font-bold text-slate-900">Sin permisos</h1>
        <p className="text-sm text-slate-500">
          Tu rol no tiene acceso a esta sección del sistema.
        </p>
        <Button variant="secondary" onClick={() => navigate('/')}>
          Volver al panel
        </Button>
      </motion.div>
    </div>
  );
}
