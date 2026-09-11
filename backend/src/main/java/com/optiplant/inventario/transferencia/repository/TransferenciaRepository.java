package com.optiplant.inventario.transferencia.repository;

import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.entity.Transferencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Los 4 métodos de listado evitan parámetros nulos en el WHERE (PostgreSQL no
 * infiere el tipo de un enum/timestamptz nulo en {@code :param is null}); el
 * Service elige el método según qué filtros vengan.
 */
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Override
    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino"})
    Optional<Transferencia> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino"})
    Page<Transferencia> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino"})
    Page<Transferencia> findByEstado(EstadoTransferencia estado, Pageable pageable);

    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino"})
    @Query("""
            select t from Transferencia t
            where t.sucursalOrigen.id = :branchId or t.sucursalDestino.id = :branchId
            """)
    Page<Transferencia> findByBranch(@Param("branchId") Long branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"producto", "sucursalOrigen", "sucursalDestino"})
    @Query("""
            select t from Transferencia t
            where t.estado = :estado
              and (t.sucursalOrigen.id = :branchId or t.sucursalDestino.id = :branchId)
            """)
    Page<Transferencia> findByEstadoAndBranch(@Param("estado") EstadoTransferencia estado,
                                              @Param("branchId") Long branchId,
                                              Pageable pageable);
}
