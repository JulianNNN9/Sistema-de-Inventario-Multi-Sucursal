package com.optiplant.inventario.venta.repository;

import com.optiplant.inventario.venta.entity.ListaPrecio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Long> {

    @Override
    @EntityGraph(attributePaths = {"sucursal", "detalles", "detalles.producto"})
    Optional<ListaPrecio> findById(Long id);

    /**
     * Listas visibles para una sucursal: las globales + las suyas.
     * {@code sucursalId} nulo = todas (ADMIN_GENERAL). Conjunto de baja
     * cardinalidad (config de precios); se acepta la paginación en memoria del
     * {@code @EntityGraph} sobre colección.
     */
    @EntityGraph(attributePaths = {"sucursal", "detalles", "detalles.producto"})
    @Query("""
            select distinct l from ListaPrecio l
            where :sucursalId is null or l.sucursal is null or l.sucursal.id = :sucursalId
            """)
    Page<ListaPrecio> findVisibles(@Param("sucursalId") Long sucursalId, Pageable pageable);
}
