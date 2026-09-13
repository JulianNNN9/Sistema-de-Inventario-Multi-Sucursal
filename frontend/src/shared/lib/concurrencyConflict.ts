/**
 * Mensaje exacto que devuelve el backend (409) cuando una escritura pierde el
 * chequeo de versión optimista sobre una fila (@Version, GlobalExceptionHandler).
 * Se usa para distinguir este caso de cualquier otro 409 de negocio, ya que
 * ApiErrorResponse no trae un código de error, solo el mensaje en español.
 */
export const CONCURRENCY_CONFLICT_MESSAGE =
  'El inventario fue modificado por otra operación al mismo tiempo. Intenta nuevamente.';

/** Evento global disparado por el cliente Axios para avisar de este conflicto fuera del árbol de React. */
export const CONCURRENCY_CONFLICT_EVENT = 'optiplant:concurrency-conflict';

/** true si una respuesta de error del backend corresponde a este conflicto puntual. */
export function isConcurrencyConflictError(status: number | undefined, message: string): boolean {
  return status === 409 && message === CONCURRENCY_CONFLICT_MESSAGE;
}
