import { useEffect, useState } from 'react';
import { getSale } from '../api/ventas';
import type { ApiError } from '../types/api';
import type { Sale } from '../types/venta';

export function useVenta(id: number | null) {
  const [sale, setSale] = useState<Sale | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id === null) {
      setSale(null);
      return;
    }
    let active = true;
    setLoading(true);
    getSale(id)
      .then((res) => {
        if (active) {
          setSale(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar el comprobante');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [id]);

  return { sale, loading, error };
}
