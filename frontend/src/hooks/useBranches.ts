import { useEffect, useState } from 'react';
import { listBranches } from '../api/sucursales';
import type { ApiError } from '../types/api';
import type { Sucursal } from '../types/sucursal';

export function useBranches() {
  const [branches, setBranches] = useState<Sucursal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listBranches()
      .then((res) => {
        if (active) {
          setBranches(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudieron cargar las sucursales');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return { branches, loading, error };
}
