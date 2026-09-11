import { useCallback, useEffect, useState } from 'react';
import { listPriceLists } from '../api/priceLists';
import type { ApiError } from '../types/api';
import type { PriceList } from '../types/venta';

export function usePriceLists() {
  const [priceLists, setPriceLists] = useState<PriceList[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listPriceLists()
      .then((res) => {
        if (active) {
          setPriceLists(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las listas de precios');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [reloadTick]);

  return { priceLists, loading, error, refetch };
}
