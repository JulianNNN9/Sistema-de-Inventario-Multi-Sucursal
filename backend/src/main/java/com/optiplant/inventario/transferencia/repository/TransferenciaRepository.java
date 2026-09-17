package com.optiplant.inventario.transferencia.repository;

import com.optiplant.inventario.dashboard.dto.ActiveTransfersCount;
import com.optiplant.inventario.logistica.dto.ComplianceReportRow;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.entity.Transferencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Los filtros usan {@code coalesce(:param, columna)} en vez de {@code :param is
 * null} — PostgreSQL no siempre infiere el tipo de un parámetro nulo aislado
 * (enum/timestamptz); envuelto en {@code coalesce} junto a una columna tipada
 * sí lo infiere.
 */
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Override
    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino", "transportista"})
    Optional<Transferencia> findById(Long id);

    /**
     * Bandeja de transferencias (RF-24), filtrada por estado/sucursal; el orden lo
     * aporta el Pageable. {@code soloActivas} alterna entre las que aún requieren
     * alguna acción y las que ya terminaron definitivamente (Sección 3,
     * {@link EstadoTransferencia#NO_TERMINALES}).
     */
    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino", "transportista"})
    @Query("""
            select t from Transferencia t
            where t.estado = coalesce(:estado, t.estado)
              and (:branchId is null or t.sucursalOrigen.id = :branchId or t.sucursalDestino.id = :branchId)
              and (:soloActivas = false or t.estado in :estadosNoTerminales)
              and (:soloActivas = true or t.estado not in :estadosNoTerminales)
            """)
    Page<Transferencia> search(@Param("estado") EstadoTransferencia estado,
                               @Param("branchId") Long branchId,
                               @Param("soloActivas") boolean soloActivas,
                               @Param("estadosNoTerminales") List<EstadoTransferencia> estadosNoTerminales,
                               Pageable pageable);

    /**
     * RF-23, {@code sort=priority}: ALTA &gt; MEDIA &gt; BAJA. No es expresable con un
     * {@link org.springframework.data.domain.Sort} estándar porque {@code urgencia}
     * se mapea como enum STRING (orden alfabético ≠ orden de severidad).
     */
    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino", "transportista"})
    @Query("""
            select t from Transferencia t
            where t.estado = coalesce(:estado, t.estado)
              and (:branchId is null or t.sucursalOrigen.id = :branchId or t.sucursalDestino.id = :branchId)
              and (:soloActivas = false or t.estado in :estadosNoTerminales)
              and (:soloActivas = true or t.estado not in :estadosNoTerminales)
            order by case t.urgencia when 'ALTA' then 0 when 'MEDIA' then 1 else 2 end
            """)
    Page<Transferencia> searchOrderByPriority(@Param("estado") EstadoTransferencia estado,
                                              @Param("branchId") Long branchId,
                                              @Param("soloActivas") boolean soloActivas,
                                              @Param("estadosNoTerminales") List<EstadoTransferencia> estadosNoTerminales,
                                              Pageable pageable);

    /**
     * RF-25 (Módulo 5): reporte de cumplimiento por ruta (proxy = sucursal origen +
     * transportista). Agregación en base de datos (RNF-01); {@code route} filtra por
     * transportista. La desviación (horas) es {@code fecha_real_llegada - fecha_estimada_llegada};
     * positiva = tardanza promedio, negativa = adelanto promedio, nula si ninguna
     * transferencia de esa ruta ha llegado todavía.
     */
    @Query(value = """
            select
                t.sucursal_origen_id as "sucursalOrigenId",
                s.nombre as "sucursalOrigenNombre",
                tr.nombre as "transportista",
                count(*) as "cantidad",
                avg(extract(epoch from (t.fecha_real_llegada - t.fecha_estimada_llegada)) / 3600.0) as "desviacionPromedioHoras"
            from transferencia t
            join sucursal s on s.id = t.sucursal_origen_id
            join transportista tr on tr.id = t.transportista_id
            where t.sucursal_origen_id = coalesce(:branchId, t.sucursal_origen_id)
              and tr.nombre = coalesce(:route, tr.nombre)
            group by t.sucursal_origen_id, s.nombre, tr.nombre
            order by t.sucursal_origen_id, tr.nombre
            """, nativeQuery = true)
    List<ComplianceReportRow> complianceReport(@Param("branchId") Long branchId, @Param("route") String route);

    /**
     * RF-28 (Dashboard, Módulo 6): conteo por estado no terminal, agregado en
     * BD (RNF-01). {@code estadosActivos} lo fija el Service (los 4 estados no
     * terminales del modelo, Sección 3), no es un filtro libre del cliente.
     */
    @Query("""
            select new com.optiplant.inventario.dashboard.dto.ActiveTransfersCount(t.estado, count(t))
            from Transferencia t
            where t.estado in :estadosActivos
              and (:branchId is null or t.sucursalOrigen.id = :branchId or t.sucursalDestino.id = :branchId)
            group by t.estado
            """)
    List<ActiveTransfersCount> countActivasPorEstado(@Param("estadosActivos") List<EstadoTransferencia> estadosActivos,
                                                     @Param("branchId") Long branchId);
}
