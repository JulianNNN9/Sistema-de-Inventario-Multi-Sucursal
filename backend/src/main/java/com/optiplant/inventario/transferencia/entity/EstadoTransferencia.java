package com.optiplant.inventario.transferencia.entity;

import java.util.List;

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
    CERRADA_RECLAMACION;

    /**
     * Estados no terminales del ciclo de vida de una transferencia — los que
     * "aún requieren cualquier tipo de acción", en contraste con los que ya
     * terminaron definitivamente. REENVIO_SOLICITADO se cuenta como terminal
     * a propósito: sobre ese registro puntual ya no hay ninguna acción posible
     * (ningún botón se habilita en ese estado); el faltante sigue su curso en
     * la nueva transferencia que ya se generó, que es la que de verdad requiere
     * acción y aparece por su cuenta como PENDIENTE.
     */
    public static final List<EstadoTransferencia> NO_TERMINALES =
            List.of(PENDIENTE, EN_TRANSITO, CON_FALTANTES);

    /** Nombre en español natural, para usar en mensajes dirigidos al usuario final. */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE -> "pendiente";
            case RECHAZADA -> "rechazada";
            case EN_TRANSITO -> "en tránsito";
            case COMPLETADA -> "completada";
            case CON_FALTANTES -> "con faltantes";
            case REENVIO_SOLICITADO -> "con reenvío solicitado";
            case CERRADA_AJUSTE -> "cerrada por ajuste";
            case CERRADA_RECLAMACION -> "cerrada por reclamación";
        };
    }
}
