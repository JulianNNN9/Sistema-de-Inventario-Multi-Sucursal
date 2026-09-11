package com.optiplant.inventario.transferencia.entity;

/**
 * Estados de una transferencia (Sección 3). Las transiciones válidas están
 * fijadas por el diagrama de actividad del roadmap; no se inventan estados
 * intermedios adicionales.
 */
public enum EstadoTransferencia {
    PENDIENTE,
    RECHAZADA,
    EN_TRANSITO,
    COMPLETADA,
    CON_FALTANTES,
    REENVIO_SOLICITADO,
    CERRADA_AJUSTE,
    CERRADA_RECLAMACION
}
