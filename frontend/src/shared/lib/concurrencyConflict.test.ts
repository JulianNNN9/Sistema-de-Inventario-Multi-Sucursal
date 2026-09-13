import { describe, expect, it } from 'vitest';
import { CONCURRENCY_CONFLICT_MESSAGE, isConcurrencyConflictError } from './concurrencyConflict';

describe('isConcurrencyConflictError', () => {
  it('reconoce el 409 exacto de bloqueo optimista', () => {
    expect(isConcurrencyConflictError(409, CONCURRENCY_CONFLICT_MESSAGE)).toBe(true);
  });

  it('no confunde otro 409 de negocio con un choque de bloqueo optimista', () => {
    expect(isConcurrencyConflictError(409, 'No hay stock suficiente para completar la venta')).toBe(false);
  });

  it('no confunde el mismo mensaje con otro status', () => {
    expect(isConcurrencyConflictError(400, CONCURRENCY_CONFLICT_MESSAGE)).toBe(false);
  });
});
