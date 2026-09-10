package com.optiplant.inventario.common.exception;

/**
 * HTTP 400 — reglas de negocio de validación que no cubre Bean Validation
 * (ej. una lista de precios que no contiene el producto solicitado). El mensaje
 * debe enumerar el/los campo(s) inválido(s) y la razón (RNF-04).
 */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String message) {
        super(message);
    }
}
