import { type HTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

/** Placeholder animado que reproduce la forma del dato que va a cargar. */
export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden
      className={cn('animate-pulse rounded-md bg-slate-200/80', className)}
      {...props}
    />
  );
}
