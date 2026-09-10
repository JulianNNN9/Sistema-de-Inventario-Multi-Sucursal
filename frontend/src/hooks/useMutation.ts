import { useCallback, useState } from 'react';
import type { ApiError } from '../types/api';

interface MutationState {
  submitting: boolean;
  error: string | null;
}

/**
 * Encapsula una llamada de escritura a la API con estado de envío y error.
 * El componente nunca llama a axios directamente (Sección 6 / RNF-08).
 */
export function useMutation<TArgs extends unknown[], TResult>(
  fn: (...args: TArgs) => Promise<TResult>,
) {
  const [state, setState] = useState<MutationState>({ submitting: false, error: null });

  const mutate = useCallback(
    async (...args: TArgs): Promise<TResult> => {
      setState({ submitting: true, error: null });
      try {
        const result = await fn(...args);
        setState({ submitting: false, error: null });
        return result;
      } catch (err) {
        setState({ submitting: false, error: (err as ApiError).message ?? 'La operación falló' });
        throw err;
      }
    },
    [fn],
  );

  const resetError = useCallback(() => setState((s) => ({ ...s, error: null })), []);

  return { mutate, submitting: state.submitting, error: state.error, resetError };
}
