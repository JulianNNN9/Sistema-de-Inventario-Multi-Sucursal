import { useCallback, useEffect, useState } from 'react';
import { listProductUnits } from '../api/productos';
import type { ApiError } from '../types/api';
import type { ProductoUnidad } from '../types/producto';

export function useProductUnits(productId: number | null) {
  const [units, setUnits] = useState<ProductoUnidad[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    if (productId === null) {
      setUnits([]);
      return;
    }
    let active = true;
    setLoading(true);
    listProductUnits(productId)
      .then((res) => {
        if (active) {
          setUnits(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las unidades');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [productId, reloadTick]);

  return { units, loading, error, refetch };
}
