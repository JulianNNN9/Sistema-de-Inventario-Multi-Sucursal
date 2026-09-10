import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import App from './App';

describe('App (Fase 0.E)', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redirige a la pantalla de login cuando no hay sesión', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument();
    expect(screen.getByText(/Inventario Multi-Sucursal/i)).toBeInTheDocument();
  });
});
