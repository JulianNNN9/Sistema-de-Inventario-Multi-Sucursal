import { useCallback, useEffect, useState } from 'react';
import { listSuppliers } from '../api/proveedores';
import type { ApiError } from '../../../shared/types/api';
import type { Proveedor } from '../types/compra';

export function useProveedores() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listSuppliers()
      .then((res) => {
        if (active) {
          setProveedores(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar los proveedores');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [reloadTick]);

  return { proveedores, loading, error, refetch };
}
