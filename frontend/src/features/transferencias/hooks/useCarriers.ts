import { useCallback, useEffect, useState } from 'react';
import { listCarriers } from '../api/carriers';
import type { ApiError } from '../../../shared/types/api';
import type { Transportista } from '../types/transferencia';

export function useCarriers() {
  const [carriers, setCarriers] = useState<Transportista[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listCarriers()
      .then((res) => {
        if (active) {
          setCarriers(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar los transportistas');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [reloadTick]);

  return { carriers, loading, error, refetch };
}
