package com.optiplant.inventario.common.exception;

/**
 * HTTP 409 — mensaje fijo: "Stock insuficiente para completar la operación"
 * (RNF-04). Se lanza en ventas y despachos de transferencia (RF-14, RF-18, RN-01).
 */
public class StockInsuficienteException extends RuntimeException {

    public static final String MESSAGE = "Stock insuficiente para completar la operación";

    public StockInsuficienteException() {
        super(MESSAGE);
    }
}
