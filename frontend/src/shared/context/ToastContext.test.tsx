import { act, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ToastProvider } from './ToastContext';
import { CONCURRENCY_CONFLICT_EVENT, CONCURRENCY_CONFLICT_MESSAGE } from '../lib/concurrencyConflict';

describe('ToastProvider', () => {
  it('muestra una alerta cuando el cliente Axios avisa un choque de bloqueo optimista', () => {
    render(
      <ToastProvider>
        <div />
      </ToastProvider>,
    );

    act(() => {
      window.dispatchEvent(
        new CustomEvent(CONCURRENCY_CONFLICT_EVENT, { detail: CONCURRENCY_CONFLICT_MESSAGE }),
      );
    });

    expect(screen.getByRole('status')).toHaveTextContent(CONCURRENCY_CONFLICT_MESSAGE);
  });
});
