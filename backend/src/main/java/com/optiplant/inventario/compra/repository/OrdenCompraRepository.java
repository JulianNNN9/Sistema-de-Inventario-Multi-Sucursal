package com.optiplant.inventario.compra.repository;

import com.optiplant.inventario.compra.entity.OrdenCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    /** Trae proveedor, sucursal y líneas (con su producto) en una sola consulta. */
    @Override
    @EntityGraph(attributePaths = {"proveedor", "sucursal", "detalles", "detalles.producto"})
    Optional<OrdenCompra> findById(Long id);

    /**
     * Histórico de compras con filtros opcionales por proveedor y por producto
     * (RF-11). Agregación/filtrado en BD y paginado (RNF-01).
     */
    @EntityGraph(attributePaths = {"proveedor", "sucursal"})
    @Query("""
            select o from OrdenCompra o
            where (:supplierId is null or o.proveedor.id = :supplierId)
              and (:productId is null or exists (
                    select 1 from OrdenCompraDetalle d
                    where d.orden = o and d.producto.id = :productId))
            """)
    Page<OrdenCompra> search(@Param("supplierId") Long supplierId,
                             @Param("productId") Long productId,
                             Pageable pageable);
}
