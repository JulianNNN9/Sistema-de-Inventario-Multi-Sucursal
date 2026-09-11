import { useCallback, useEffect, useState } from 'react';
import { listSales } from '../api/ventas';
import type { ApiError, PageResponse } from '../types/api';
import type { SaleSummary } from '../types/venta';

interface Options {
  page?: number;
  size?: number;
  branchId?: number;
  from?: string;
  to?: string;
}

export function useVentasList({ page = 0, size = 20, branchId, from, to }: Options = {}) {
  const [data, setData] = useState<PageResponse<SaleSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listSales({ page, size, branchId, from: from || undefined, to: to || undefined })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar el historial de ventas');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, size, branchId, from, to, reloadTick]);

  return { data, loading, error, refetch };
}
