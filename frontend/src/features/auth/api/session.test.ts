import { describe, expect, it } from 'vitest';
import { isTokenExpired } from './session';

/** Construye un JWT de juguete (header.payload.signature) con el `exp` dado. */
function fakeJwt(expSeconds: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: '1', exp: expSeconds }));
  return `${header}.${payload}.sig`;
}

describe('isTokenExpired', () => {
  it('devuelve false para un token con exp en el futuro', () => {
    expect(isTokenExpired(fakeJwt(Math.floor(Date.now() / 1000) + 3600))).toBe(false);
  });

  it('devuelve true para un token con exp en el pasado', () => {
    expect(isTokenExpired(fakeJwt(Math.floor(Date.now() / 1000) - 10))).toBe(true);
  });

  it('devuelve true para un token no decodificable', () => {
    expect(isTokenExpired('no-es-un-jwt')).toBe(true);
  });
});
