package com.optiplant.inventario.logistica.dto;

/**
 * Proyección de la consulta nativa agregada de {@code TransferenciaRepository}
 * (RF-25). Los nombres de los getters deben coincidir con los alias entre
 * comillas de la consulta SQL.
 */
public interface ComplianceReportRow {

    Long getSucursalOrigenId();

    String getSucursalOrigenNombre();

    String getTransportista();

    Long getCantidad();

    Double getDesviacionPromedioHoras();
}
