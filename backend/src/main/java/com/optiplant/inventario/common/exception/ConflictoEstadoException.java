package com.optiplant.inventario.common.exception;

/**
 * HTTP 409 — la operación es válida en forma pero entra en conflicto con el
 * estado actual del recurso (ej. eliminar un producto con inventario asociado,
 * reconfirmar una orden de compra ya recibida). RNF-04. El mensaje es siempre
 * específico al caso.
 */
public class ConflictoEstadoException extends RuntimeException {

    public ConflictoEstadoException(String message) {
        super(message);
    }
}
