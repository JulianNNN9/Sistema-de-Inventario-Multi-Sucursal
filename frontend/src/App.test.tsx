import { render, screen } from '@testing-library/react';
import App from './App';

describe('App shell (Fase 0.A)', () => {
  it('renders the application title', () => {
    render(<App />);
    expect(
      screen.getByText(/Inventario Multi-Sucursal/i),
    ).toBeInTheDocument();
  });
});
