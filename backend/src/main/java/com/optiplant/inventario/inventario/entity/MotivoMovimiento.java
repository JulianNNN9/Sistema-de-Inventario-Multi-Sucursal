package com.optiplant.inventario.inventario.entity;

/** Motivo de un movimiento de inventario (Sección 3, RF-03/RF-04). */
public enum MotivoMovimiento {
    COMPRA,
    DEVOLUCION,
    AJUSTE,
    VENTA,
    MERMA,
    TRANSFERENCIA_SALIDA,
    TRANSFERENCIA_ENTRADA
}
