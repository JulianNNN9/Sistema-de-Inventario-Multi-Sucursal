package com.optiplant.inventario.compra.repository;

import com.optiplant.inventario.compra.dto.PurchaseOrderSummaryResponse;
import com.optiplant.inventario.compra.entity.EstadoOrdenCompra;
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
     * Histórico de compras (RF-11) con filtros opcionales por proveedor, sucursal
     * y producto. Proyección a DTO y {@code total} agregado en base de datos
     * (ya neto de descuento por línea), paginado (RNF-01: agregación en BD, sin
     * traer las filas a memoria). {@code soloActivas} alterna entre las órdenes
     * PENDIENTES (vista principal) y el resto (histórico: RECIBIDA/CANCELADA).
     */
    @Query(value = """
            select new com.optiplant.inventario.compra.dto.PurchaseOrderSummaryResponse(
                o.id, prov.id, prov.nombre, suc.id, suc.nombre, o.fecha, o.estado,
                coalesce((select sum(d.cantidad * d.precioUnitario * (1 - d.descuento / 100))
                          from OrdenCompraDetalle d where d.orden = o), 0))
            from OrdenCompra o
            join o.proveedor prov
            join o.sucursal suc
            where (:supplierId is null or prov.id = :supplierId)
              and (:branchId is null or suc.id = :branchId)
              and (:productId is null or exists (
                    select 1 from OrdenCompraDetalle dp
                    where dp.orden = o and dp.producto.id = :productId))
              and (:soloActivas = false or o.estado = 'PENDIENTE')
              and (:soloActivas = true or o.estado <> 'PENDIENTE')
            order by o.fecha desc
            """,
            countQuery = """
            select count(o) from OrdenCompra o
            where (:supplierId is null or o.proveedor.id = :supplierId)
              and (:branchId is null or o.sucursal.id = :branchId)
              and (:productId is null or exists (
                    select 1 from OrdenCompraDetalle dp
                    where dp.orden = o and dp.producto.id = :productId))
              and (:soloActivas = false or o.estado = 'PENDIENTE')
              and (:soloActivas = true or o.estado <> 'PENDIENTE')
            """)
    Page<PurchaseOrderSummaryResponse> searchSummaries(@Param("supplierId") Long supplierId,
                                                       @Param("branchId") Long branchId,
                                                       @Param("productId") Long productId,
                                                       @Param("soloActivas") boolean soloActivas,
                                                       Pageable pageable);

    /**
     * Órdenes PENDIENTES de una sucursal (worklist de OPERADOR_INVENTARIO para
     * "confirmar recepción"): no es el histórico de compras (RF-11, solo
     * ADMIN/GERENTE) — es la lista mínima de qué falta recibir en su propia
     * sucursal, sin filtros de proveedor/producto.
     */
    @Query(value = """
            select new com.optiplant.inventario.compra.dto.PurchaseOrderSummaryResponse(
                o.id, prov.id, prov.nombre, suc.id, suc.nombre, o.fecha, o.estado,
                coalesce((select sum(d.cantidad * d.precioUnitario * (1 - d.descuento / 100))
                          from OrdenCompraDetalle d where d.orden = o), 0))
            from OrdenCompra o
            join o.proveedor prov
            join o.sucursal suc
            where o.estado = :estado
              and suc.id = :branchId
            order by o.fecha asc
            """,
            countQuery = """
            select count(o) from OrdenCompra o
            where o.estado = :estado
              and o.sucursal.id = :branchId
            """)
    Page<PurchaseOrderSummaryResponse> findPendingByBranch(@Param("branchId") Long branchId,
                                                           @Param("estado") EstadoOrdenCompra estado,
                                                           Pageable pageable);
}
