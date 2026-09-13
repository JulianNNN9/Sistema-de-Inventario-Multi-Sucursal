import { useCallback, useEffect, useState } from 'react';
import { getComplianceReport } from '../api/logistica';
import type { ApiError } from '../../../shared/types/api';
import type { ComplianceReportRow } from '../types/logistica';

interface Options {
  branchId?: number;
  route?: string;
}

export function useComplianceReport({ branchId, route }: Options = {}) {
  const [data, setData] = useState<ComplianceReportRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getComplianceReport({ branchId, route: route || undefined })
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar el reporte de cumplimiento');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [branchId, route, reloadTick]);

  return { data, loading, error, refetch };
}
