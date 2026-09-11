package com.optiplant.inventario.inventario.repository;

import com.optiplant.inventario.dashboard.dto.InventoryRotationItem;
import com.optiplant.inventario.inventario.entity.MovimientoInventario;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /** Historial auditable de un inventario, paginado, con responsable cargado (RF-07, RNF-01). */
    @EntityGraph(attributePaths = {"responsable"})
    Page<MovimientoInventario> findByInventarioIdOrderByFechaDesc(Long inventarioId, Pageable pageable);

    /**
     * RF-27: rotación = suma de {@code cantidad} en movimientos {@code tipo} de
     * los últimos 30 días, agrupada por producto, agregada en BD (RNF-01). El
     * orden (desc/asc) lo fija el llamador vía {@code order by} explícito en
     * cada método; {@code Pageable} sólo aporta el LIMIT top-5.
     */
    @Query("""
            select new com.optiplant.inventario.dashboard.dto.InventoryRotationItem(
                p.id, p.sku, p.nombre, sum(m.cantidad))
            from MovimientoInventario m
              join m.inventario inv
              join inv.producto p
            where m.tipo = :tipo
              and m.fecha >= :from
              and (:branchId is null or inv.sucursal.id = :branchId)
            group by p.id, p.sku, p.nombre
            order by sum(m.cantidad) desc
            """)
    List<InventoryRotationItem> topMayorRotacion(@Param("branchId") Long branchId,
                                                 @Param("tipo") TipoMovimiento tipo,
                                                 @Param("from") Instant from,
                                                 Pageable pageable);

    @Query("""
            select new com.optiplant.inventario.dashboard.dto.InventoryRotationItem(
                p.id, p.sku, p.nombre, sum(m.cantidad))
            from MovimientoInventario m
              join m.inventario inv
              join inv.producto p
            where m.tipo = :tipo
              and m.fecha >= :from
              and (:branchId is null or inv.sucursal.id = :branchId)
            group by p.id, p.sku, p.nombre
            order by sum(m.cantidad) asc
            """)
    List<InventoryRotationItem> topMenorRotacion(@Param("branchId") Long branchId,
                                                  @Param("tipo") TipoMovimiento tipo,
                                                  @Param("from") Instant from,
                                                  Pageable pageable);
}
