/**
 * Código que expone el backend en `ApiErrorResponse.code` cuando una escritura
 * pierde el chequeo de versión optimista sobre una fila (@Version,
 * GlobalExceptionHandler.handleOptimisticLocking) en cualquier entidad que lo
 * declare, no solo inventario. Se usa para distinguir este caso de cualquier
 * otro 409 de negocio, ya que comparar el texto exacto de `message` es frágil
 * (cambia con el idioma/redacción y no identifica la causa real del error).
 */
export const CONCURRENCY_CONFLICT_CODE = 'CONCURRENCY_CONFLICT';

/** Mensaje genérico que acompaña a {@link CONCURRENCY_CONFLICT_CODE}; usado como texto de referencia en tests. */
export const CONCURRENCY_CONFLICT_MESSAGE =
  'Este registro fue modificado por otra persona al mismo tiempo. Actualiza la información e intenta nuevamente.';

/** Evento global disparado por el cliente Axios para avisar de este conflicto fuera del árbol de React. */
export const CONCURRENCY_CONFLICT_EVENT = 'optiplant:concurrency-conflict';

/** true si una respuesta de error del backend corresponde a este conflicto puntual. */
export function isConcurrencyConflictError(status: number | undefined, code: string | undefined | null): boolean {
  return status === 409 && code === CONCURRENCY_CONFLICT_CODE;
}
