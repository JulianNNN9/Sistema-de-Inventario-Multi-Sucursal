import { useCallback, useEffect, useState } from 'react';
import { listProducts } from '../api/productos';
import type { ApiError, PageResponse } from '../../../shared/types/api';
import type { Producto } from '../types/producto';

interface Options {
  page?: number;
  size?: number;
  branchId?: number;
  /** Si es false, el hook no dispara la petición (útil para modales aún cerrados). */
  enabled?: boolean;
}

export function useProductos({ page = 0, size = 20, branchId, enabled = true }: Options = {}) {
  const [data, setData] = useState<PageResponse<Producto> | null>(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    if (!enabled) return;
    let active = true;
    setLoading(true);
    listProducts({ page, size, branchId })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar los productos');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, size, branchId, reloadTick, enabled]);

  return { data, loading, error, refetch };
}
