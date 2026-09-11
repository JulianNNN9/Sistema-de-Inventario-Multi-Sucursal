import { useCallback, useEffect, useState } from 'react';
import {
  getActiveTransfers,
  getBranchComparison,
  getInventoryRotation,
  getRestockAlerts,
  getSalesComparison,
} from '../api/dashboard';
import type { ApiError } from '../types/api';
import type {
  ActiveTransfersCount,
  BranchComparisonRow,
  InventoryRotationResponse,
  RestockAlert,
  SalesComparisonPoint,
} from '../types/dashboard';

export interface DashboardData {
  sales: SalesComparisonPoint[];
  rotation: InventoryRotationResponse;
  activeTransfers: ActiveTransfersCount[];
  restockAlerts: RestockAlert[];
  branchComparison: BranchComparisonRow[] | null;
}

interface Options {
  branchId?: number;
  includeBranchComparison: boolean;
}

/** Trae los 5 indicadores del panel en paralelo (RF-26..RF-30). */
export function useDashboard({ branchId, includeBranchComparison }: Options) {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const refetch = useCallback(() => setReloadTick((t) => t + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([
      getSalesComparison({ branchId }),
      getInventoryRotation({ branchId }),
      getActiveTransfers({ branchId }),
      getRestockAlerts({ branchId }),
      includeBranchComparison ? getBranchComparison() : Promise.resolve(null),
    ])
      .then(([sales, rotation, activeTransfers, restockAlerts, branchComparison]) => {
        if (active) {
          setData({ sales, rotation, activeTransfers, restockAlerts, branchComparison });
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar el panel');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [branchId, includeBranchComparison, reloadTick]);

  return { data, loading, error, refetch };
}
