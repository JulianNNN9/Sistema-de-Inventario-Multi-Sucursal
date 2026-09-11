package com.optiplant.inventario.venta.repository;

import com.optiplant.inventario.venta.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Comprobante: venta con sucursal, usuario y líneas (con su producto). */
    @Override
    @EntityGraph(attributePaths = {"sucursal", "usuario", "detalles", "detalles.producto"})
    Optional<Venta> findById(Long id);

    /**
     * Histórico de ventas con filtro opcional por sucursal y rango de fechas
     * (RF-16). El rango siempre llega acotado desde el Service (centinelas si el
     * filtro no viene) para evitar parámetros {@code timestamptz} nulos en la
     * consulta. Paginado, sucursal y usuario cargados (RNF-01).
     */
    @EntityGraph(attributePaths = {"sucursal", "usuario"})
    @Query("""
            select v from Venta v
            where (:branchId is null or v.sucursal.id = :branchId)
              and v.fecha >= :from
              and v.fecha < :to
            order by v.fecha desc
            """)
    Page<Venta> search(@Param("branchId") Long branchId,
                       @Param("from") Instant from,
                       @Param("to") Instant to,
                       Pageable pageable);

    /** Suma agregada en BD (RNF-01) para un rango; usada por el Dashboard (RF-26). */
    @Query("""
            select coalesce(sum(v.total), 0)
            from Venta v
            where (:branchId is null or v.sucursal.id = :branchId)
              and v.fecha >= :from
              and v.fecha < :to
            """)
    BigDecimal sumTotalEntreFechas(@Param("branchId") Long branchId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);
}
