import { describe, expect, it } from 'vitest';
import { CONCURRENCY_CONFLICT_CODE, isConcurrencyConflictError } from './concurrencyConflict';

describe('isConcurrencyConflictError', () => {
  it('reconoce el 409 con el código exacto de bloqueo optimista', () => {
    expect(isConcurrencyConflictError(409, CONCURRENCY_CONFLICT_CODE)).toBe(true);
  });

  it('no confunde otro 409 de negocio (sin code) con un choque de bloqueo optimista', () => {
    expect(isConcurrencyConflictError(409, undefined)).toBe(false);
  });

  it('no confunde otro código con un choque de bloqueo optimista', () => {
    expect(isConcurrencyConflictError(409, 'OTRO_CODIGO')).toBe(false);
  });

  it('no confunde el mismo código con otro status', () => {
    expect(isConcurrencyConflictError(400, CONCURRENCY_CONFLICT_CODE)).toBe(false);
  });
});
