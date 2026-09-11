package com.optiplant.inventario.sucursal.repository;

import com.optiplant.inventario.dashboard.dto.BranchComparisonRow;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    /**
     * RF-30 (Dashboard, Módulo 6, solo ADMIN_GENERAL): ventas totales del mes
     * actual y rotación agregada (RETIRO) de los últimos 30 días, por sucursal.
     * Subconsultas correlacionadas para evitar el join cartesiano de combinar
     * ventas y movimientos en una sola fila por sucursal; toda la agregación
     * ocurre en BD (RNF-01).
     */
    @Query("""
            select new com.optiplant.inventario.dashboard.dto.BranchComparisonRow(
                s.id, s.nombre,
                coalesce((select sum(v.total) from Venta v
                          where v.sucursal = s and v.fecha >= :ventasDesde and v.fecha < :ventasHasta), 0),
                coalesce((select sum(m.cantidad) from MovimientoInventario m
                          where m.inventario.sucursal = s and m.tipo = :tipoRotacion and m.fecha >= :rotacionDesde), 0))
            from Sucursal s
            order by s.nombre
            """)
    List<BranchComparisonRow> comparativaPorSucursal(@Param("ventasDesde") Instant ventasDesde,
                                                      @Param("ventasHasta") Instant ventasHasta,
                                                      @Param("tipoRotacion") TipoMovimiento tipoRotacion,
                                                      @Param("rotacionDesde") Instant rotacionDesde);
}
