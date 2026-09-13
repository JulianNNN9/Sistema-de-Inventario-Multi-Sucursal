import { Loader2 } from 'lucide-react';
import { cn } from '../../lib/cn';

export function Spinner({ className }: { className?: string }) {
  return (
    <Loader2
      role="status"
      aria-label="Cargando"
      className={cn('h-5 w-5 animate-spin text-slate-400', className)}
    />
  );
}
