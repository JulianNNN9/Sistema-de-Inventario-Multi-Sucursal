import { useCallback, useEffect, useState } from 'react';
import { listRebalanceSuggestions } from '../api/rebalanceo';
import type { ApiError } from '../types/api';
import type { Sugerencia } from '../types/rebalanceo';

interface Options {
  /** Si es false, el hook no dispara la petición (RF-31 es solo ADMIN_GENERAL). */
  enabled?: boolean;
}

export function useRebalanceSuggestions({ enabled = true }: Options = {}) {
  const [data, setData] = useState<Sugerencia[]>([]);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    if (!enabled) return;
    let active = true;
    setLoading(true);
    listRebalanceSuggestions()
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las sugerencias de rebalanceo');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [enabled, reloadTick]);

  return { data, loading, error, refetch };
}
