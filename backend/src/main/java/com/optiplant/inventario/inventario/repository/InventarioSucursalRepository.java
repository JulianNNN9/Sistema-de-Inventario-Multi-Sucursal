package com.optiplant.inventario.inventario.repository;

import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventarioSucursalRepository extends JpaRepository<InventarioSucursal, Long> {

    Optional<InventarioSucursal> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    boolean existsByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    boolean existsByProductoId(Long productoId);

    /** Inventario de una sucursal, paginado, con producto y sucursal ya cargados (RNF-01). */
    @EntityGraph(attributePaths = {"producto", "sucursal"})
    Page<InventarioSucursal> findBySucursalId(Long sucursalId, Pageable pageable);

    /**
     * RF-29: productos cuya existencia ya alcanzó (o bajó) el mínimo. Sin
     * paginar por diseño (reporte acotado por catálogo de la sucursal, igual
     * criterio que el reporte de cumplimiento del Módulo 5).
     */
    @EntityGraph(attributePaths = {"producto", "sucursal"})
    @Query("""
            select inv from InventarioSucursal inv
            where inv.cantidadActual <= inv.stockMinimo
              and (:branchId is null or inv.sucursal.id = :branchId)
            order by inv.sucursal.nombre, inv.producto.nombre
            """)
    List<InventarioSucursal> restockAlerts(@Param("branchId") Long branchId);
}
