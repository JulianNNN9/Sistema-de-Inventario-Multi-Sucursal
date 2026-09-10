package com.optiplant.inventario.inventario.repository;

import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventarioSucursalRepository extends JpaRepository<InventarioSucursal, Long> {

    Optional<InventarioSucursal> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    boolean existsByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    /** Inventario de una sucursal, paginado, con producto y sucursal ya cargados (RNF-01). */
    @EntityGraph(attributePaths = {"producto", "sucursal"})
    Page<InventarioSucursal> findBySucursalId(Long sucursalId, Pageable pageable);
}
