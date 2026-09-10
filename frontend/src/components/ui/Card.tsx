import { type HTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  hover?: boolean;
}

export function Card({ hover = false, className, ...props }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm',
        hover && 'transition-all duration-200 ease-in-out hover:border-slate-300 hover:shadow-md',
        className,
      )}
      {...props}
    />
  );
}
