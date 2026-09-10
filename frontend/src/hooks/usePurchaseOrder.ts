import { useEffect, useState } from 'react';
import { getPurchaseOrder } from '../api/compras';
import type { ApiError } from '../types/api';
import type { PurchaseOrder } from '../types/compra';

export function usePurchaseOrder(id: number | null) {
  const [order, setOrder] = useState<PurchaseOrder | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id === null) {
      setOrder(null);
      return;
    }
    let active = true;
    setLoading(true);
    getPurchaseOrder(id)
      .then((res) => {
        if (active) {
          setOrder(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar la orden');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [id]);

  return { order, loading, error };
}
