import { useCallback, useEffect, useState } from 'react';
import { listBranches } from '../api/sucursales';
import type { ApiError } from '../../../shared/types/api';
import type { Sucursal } from '../types/sucursal';

export function useBranches() {
  const [branches, setBranches] = useState<Sucursal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listBranches()
      .then((res) => {
        if (active) {
          setBranches(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las sucursales');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [reloadTick]);

  return { branches, loading, error, refetch };
}
