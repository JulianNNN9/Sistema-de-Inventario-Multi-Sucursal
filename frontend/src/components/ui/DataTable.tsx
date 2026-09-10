import { type ReactNode } from 'react';
import { Skeleton } from './Skeleton';
import { cn } from '../../lib/cn';

type Align = 'left' | 'right' | 'center';

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  align?: Align;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  loading?: boolean;
  skeletonRows?: number;
  empty?: ReactNode;
}

function alignClass(align?: Align): string {
  if (align === 'right') return 'text-right';
  if (align === 'center') return 'text-center';
  return 'text-left';
}

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  loading = false,
  skeletonRows = 6,
  empty,
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200/80 bg-white">
      <table className="w-full min-w-[640px] text-sm">
        <thead>
          <tr className="border-b border-slate-100 text-xs font-medium uppercase tracking-wide text-slate-500">
            {columns.map((column) => (
              <th key={column.key} className={cn('px-4 py-3', alignClass(column.align))}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-50">
          {loading &&
            Array.from({ length: skeletonRows }).map((_, index) => (
              <tr key={`skeleton-${index}`}>
                {columns.map((column) => (
                  <td key={column.key} className="px-4 py-3">
                    <Skeleton className="h-4 w-full max-w-[160px]" />
                  </td>
                ))}
              </tr>
            ))}

          {!loading && rows.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="px-4 py-10 text-center">
                {empty ?? <span className="text-sm text-slate-500">Sin resultados</span>}
              </td>
            </tr>
          )}

          {!loading &&
            rows.map((row) => (
              <tr key={rowKey(row)} className="transition-colors hover:bg-slate-50/60">
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={cn(
                      'px-4 py-3 text-slate-700',
                      alignClass(column.align),
                      column.className,
                    )}
                  >
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}
