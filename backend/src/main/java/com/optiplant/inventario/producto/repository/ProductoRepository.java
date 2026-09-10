package com.optiplant.inventario.producto.repository;

import com.optiplant.inventario.producto.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Catálogo visible para una sucursal: productos que tienen existencias
     * registradas en ella (RF-01). Agregación en BD, paginada (RNF-01).
     */
    @Query("""
            select p from Producto p
            where exists (
                select 1 from InventarioSucursal i
                where i.producto = p and i.sucursal.id = :sucursalId
            )
            """)
    Page<Producto> findAllInSucursal(@Param("sucursalId") Long sucursalId, Pageable pageable);
}
