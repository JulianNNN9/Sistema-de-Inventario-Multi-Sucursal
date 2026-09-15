package com.optiplant.inventario.producto;

import java.util.List;

/**
 * Catálogo cerrado de unidades de medida base válidas para un producto
 * (RF-01). Antes {@code unidadMedidaBase} era texto libre y permitía valores
 * sin sentido (p.ej. "banano"); ahora el alta/edición solo acepta uno de
 * estos valores, reflejados también en el desplegable del frontend.
 */
public final class UnidadesMedida {

    private UnidadesMedida() {
    }

    public static final List<String> VALORES = List.of(
            "unidad", "kg", "g", "litro", "ml", "metro", "caja", "paquete", "par", "galón", "docena", "rollo");

    /** Expresión regular para {@code @Pattern}; debe reflejar {@link #VALORES}. */
    public static final String REGEX =
            "unidad|kg|g|litro|ml|metro|caja|paquete|par|galón|docena|rollo";
}
