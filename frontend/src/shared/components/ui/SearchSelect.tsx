import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { ChevronsUpDown } from 'lucide-react';
import { cn } from '../../lib/cn';

export interface SearchSelectOption {
  value: string | number;
  /** Texto principal mostrado en la lista y usado para filtrar (ej. "SKU-0001 · Martillo"). */
  label: string;
}

interface SearchSelectProps {
  label?: string;
  hint?: string;
  error?: string;
  options: SearchSelectOption[];
  value: string | number | '';
  onChange: (value: string) => void;
  placeholder?: string;
  loading?: boolean;
  disabled?: boolean;
  required?: boolean;
  emptyMessage?: string;
}

/** Combobox filtrable: escribe para buscar (ej. por SKU) y elige de la lista. */
export function SearchSelect({
  label,
  hint,
  error,
  options,
  value,
  onChange,
  placeholder = 'Buscar…',
  loading = false,
  disabled = false,
  required = false,
  emptyMessage = 'Sin resultados',
}: SearchSelectProps) {
  const generatedId = useId();
  const hintId = hint ? `${generatedId}-hint` : undefined;
  const rootRef = useRef<HTMLDivElement>(null);

  const selected = useMemo(() => options.find((o) => String(o.value) === String(value)) ?? null, [options, value]);

  const [query, setQuery] = useState(selected?.label ?? '');
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      setQuery(selected?.label ?? '');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected?.label, isOpen]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setQuery(selected?.label ?? '');
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [selected?.label]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!isOpen || !q || q === selected?.label.toLowerCase()) return options;
    return options.filter((o) => o.label.toLowerCase().includes(q));
  }, [options, query, isOpen, selected?.label]);

  function selectOption(option: SearchSelectOption) {
    onChange(String(option.value));
    setQuery(option.label);
    setIsOpen(false);
  }

  return (
    <div className="space-y-1.5" ref={rootRef}>
      {label && (
        <label htmlFor={generatedId} className="block text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <div className="relative">
        <input
          id={generatedId}
          role="combobox"
          aria-expanded={isOpen}
          aria-controls={`${generatedId}-listbox`}
          aria-autocomplete="list"
          aria-describedby={hintId}
          aria-invalid={error ? true : undefined}
          autoComplete="off"
          className={cn(
            'h-10 w-full rounded-xl border bg-white px-3 pr-9 text-sm text-slate-900 placeholder:text-slate-400',
            'transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:ring-offset-white',
            error ? 'border-rose-300 focus-visible:ring-rose-400' : 'border-slate-200 focus-visible:ring-brand-500',
            disabled && 'cursor-not-allowed bg-slate-50 text-slate-400',
          )}
          placeholder={loading ? 'Cargando…' : placeholder}
          value={query}
          disabled={disabled || loading}
          required={required}
          onFocus={() => setIsOpen(true)}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
            if (value !== '') onChange('');
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape') {
              setIsOpen(false);
              setQuery(selected?.label ?? '');
            }
          }}
        />
        <ChevronsUpDown
          className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
          aria-hidden
        />
        {isOpen && !loading && (
          <ul
            id={`${generatedId}-listbox`}
            role="listbox"
            className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-xl border border-slate-200 bg-white py-1 shadow-lg"
          >
            {filtered.length === 0 ? (
              <li className="px-3 py-2 text-sm text-slate-500">{emptyMessage}</li>
            ) : (
              filtered.map((option) => (
                <li
                  key={option.value}
                  role="option"
                  aria-selected={String(option.value) === String(value)}
                  className={cn(
                    'cursor-pointer px-3 py-2 text-sm text-slate-700 hover:bg-brand-50',
                    String(option.value) === String(value) && 'bg-brand-50 font-medium text-brand-700',
                  )}
                  onMouseDown={(e) => {
                    e.preventDefault();
                    selectOption(option);
                  }}
                >
                  {option.label}
                </li>
              ))
            )}
          </ul>
        )}
      </div>
      {!error && hint && (
        <p id={hintId} className="text-xs text-slate-500">
          {hint}
        </p>
      )}
      {error && <p className="text-xs font-medium text-rose-600">{error}</p>}
    </div>
  );
}
