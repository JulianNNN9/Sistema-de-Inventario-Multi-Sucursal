package com.optiplant.inventario.common.exception;

/**
 * HTTP 409 — el mensaje describe la transición de estado rechazada
 * (ej. "La transferencia no puede prepararse porque aún no ha sido aprobada").
 * RNF-04, Módulo 4.
 */
public class TransferenciaInvalidaException extends RuntimeException {

    public TransferenciaInvalidaException(String message) {
        super(message);
    }
}
