import { useEffect, useState } from 'react';
import { getTransferEvents } from '../api/transferencias';
import type { ApiError } from '../types/api';
import type { TransferEvent } from '../types/transferencia';

export function useTransferEvents(id: number | null) {
  const [events, setEvents] = useState<TransferEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id === null) {
      setEvents([]);
      return;
    }
    let active = true;
    setLoading(true);
    getTransferEvents(id)
      .then((res) => {
        if (active) {
          setEvents(res.content);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (active) setError(err.message ?? 'No se pudo cargar el historial');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [id]);

  return { events, loading, error };
}
