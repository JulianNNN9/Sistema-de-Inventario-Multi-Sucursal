package com.optiplant.inventario.rebalanceo.strategy;

import com.optiplant.inventario.rebalanceo.dto.Sugerencia;

import java.util.List;

/**
 * Patrón Strategy (apartado 8.2 del roadmap): encapsula el algoritmo de
 * cálculo de sugerencias de rebalanceo (RF-31), con {@link DeficitSuperavitStrategy}
 * como única implementación actual. Se declara literalmente así (no solo en
 * la justificación escrita) para poder sustituir el algoritmo sin tocar
 * {@code RebalanceoService}.
 */
public interface RebalanceoStrategy {

    List<Sugerencia> calcular();
}
