package com.optiplant.inventario.common.exception;

/**
 * HTTP 404 — mensaje: "&lt;Entidad&gt; con id &lt;id&gt; no encontrado" (RNF-04).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String entidad, Object id) {
        super(entidad + " con id " + id + " no encontrado");
    }
}
