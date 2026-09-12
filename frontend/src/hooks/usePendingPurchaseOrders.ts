import { useCallback, useEffect, useState } from 'react';
import { listPendingPurchaseOrders } from '../api/compras';
import type { ApiError, PageResponse } from '../types/api';
import type { PurchaseOrderSummary } from '../types/compra';

interface Options {
  page?: number;
  size?: number;
}

/** Worklist de OPERADOR_INVENTARIO: órdenes PENDIENTES de su propia sucursal. */
export function usePendingPurchaseOrders({ page = 0, size = 20 }: Options = {}) {
  const [data, setData] = useState<PageResponse<PurchaseOrderSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listPendingPurchaseOrders({ page, size })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las órdenes pendientes');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, size, reloadTick]);

  return { data, loading, error, refetch };
}
