import { AlertTriangle } from 'lucide-react';
import { cn } from '../lib/cn';

interface ErrorAlertProps {
  message: string;
  className?: string;
}

/**
 * Presentacional puro. Muestra el campo `message` de la respuesta de error del
 * backend tal cual, sin transformarlo (RNF-04).
 */
export function ErrorAlert({ message, className }: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className={cn(
        'flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700',
        className,
      )}
    >
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <span>{message}</span>
    </div>
  );
}
