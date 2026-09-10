import { useCallback, useEffect, useState } from 'react';
import { listPurchaseOrders } from '../api/compras';
import type { ApiError, PageResponse } from '../types/api';
import type { PurchaseOrderSummary } from '../types/compra';

interface Options {
  page?: number;
  size?: number;
  supplierId?: number;
  productId?: number;
  branchId?: number;
}

export function useComprasList({ page = 0, size = 20, supplierId, productId, branchId }: Options = {}) {
  const [data, setData] = useState<PageResponse<PurchaseOrderSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listPurchaseOrders({ page, size, supplierId, productId, branchId })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las órdenes de compra');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, size, supplierId, productId, branchId, reloadTick]);

  return { data, loading, error, refetch };
}
