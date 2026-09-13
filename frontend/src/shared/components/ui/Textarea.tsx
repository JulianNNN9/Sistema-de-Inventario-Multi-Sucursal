import { forwardRef, useId, type TextareaHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { label, hint, error, className, id, ...props },
  ref,
) {
  const generatedId = useId();
  const textareaId = id ?? generatedId;
  const hintId = hint ? `${textareaId}-hint` : undefined;

  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={textareaId} className="block text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <textarea
        ref={ref}
        id={textareaId}
        aria-invalid={error ? true : undefined}
        aria-describedby={hintId}
        className={cn(
          'w-full rounded-xl border bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400',
          'transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1',
          error
            ? 'border-rose-300 focus-visible:ring-rose-400'
            : 'border-slate-200 focus-visible:ring-brand-500',
          className,
        )}
        {...props}
      />
      {!error && hint && (
        <p id={hintId} className="text-xs text-slate-500">
          {hint}
        </p>
      )}
      {error && <p className="text-xs font-medium text-rose-600">{error}</p>}
    </div>
  );
});
