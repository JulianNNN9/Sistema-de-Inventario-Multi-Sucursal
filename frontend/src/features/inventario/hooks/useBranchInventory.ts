import { useCallback, useEffect, useState } from 'react';
import { getBranchInventory } from '../api/inventario';
import type { ApiError, PageResponse } from '../../../shared/types/api';
import type { InventarioSucursal } from '../types/inventario';

interface Options {
  branchId: number | null;
  page?: number;
  size?: number;
  search?: string;
  soloBajoMinimo?: boolean;
}

export function useBranchInventory({ branchId, page = 0, size = 20, search, soloBajoMinimo }: Options) {
  const [data, setData] = useState<PageResponse<InventarioSucursal> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    if (branchId === null) {
      setData(null);
      return;
    }
    let active = true;
    setLoading(true);
    getBranchInventory(branchId, { page, size, search, soloBajoMinimo })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar el inventario');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [branchId, page, size, search, soloBajoMinimo, reloadTick]);

  return { data, loading, error, refetch };
}
