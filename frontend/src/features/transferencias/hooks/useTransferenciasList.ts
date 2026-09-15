import { useCallback, useEffect, useState } from 'react';
import { listTransfers } from '../api/transferencias';
import type { ApiError, PageResponse } from '../../../shared/types/api';
import type { TransferSort } from '../../logistica/types/logistica';
import type { EstadoTransferencia, Transfer } from '../types/transferencia';

interface Options {
  page?: number;
  size?: number;
  estado?: EstadoTransferencia;
  branchId?: number;
  sort?: TransferSort;
  soloActivas?: boolean;
}

export function useTransferenciasList({ page = 0, size = 20, estado, branchId, sort, soloActivas }: Options = {}) {
  const [data, setData] = useState<PageResponse<Transfer> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listTransfers({ page, size, estado, branchId, sort, soloActivas })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las transferencias');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, size, estado, branchId, sort, soloActivas, reloadTick]);

  return { data, loading, error, refetch };
}
