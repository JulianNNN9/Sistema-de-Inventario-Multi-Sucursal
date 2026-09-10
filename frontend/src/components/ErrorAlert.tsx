interface ErrorAlertProps {
  message: string;
}

/**
 * Presentacional puro. Muestra el campo `message` de la respuesta de error del
 * backend tal cual, sin transformarlo (RNF-04).
 */
export function ErrorAlert({ message }: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
    >
      {message}
    </div>
  );
}
