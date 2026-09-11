package com.optiplant.inventario.dashboard.dto;

import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;

/**
 * RF-28: conteo de transferencias por estado no terminal. Solo aparecen
 * estados con al menos una transferencia (sin relleno de ceros — el frontend
 * conoce de antemano la lista fija de estados no terminales).
 */
public record ActiveTransfersCount(
        EstadoTransferencia estado,
        Long cantidad
) {
}
