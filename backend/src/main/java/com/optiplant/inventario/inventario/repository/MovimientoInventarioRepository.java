package com.optiplant.inventario.inventario.repository;

import com.optiplant.inventario.inventario.entity.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /** Historial auditable de un inventario, paginado, con responsable cargado (RF-07, RNF-01). */
    @EntityGraph(attributePaths = {"responsable"})
    Page<MovimientoInventario> findByInventarioIdOrderByFechaDesc(Long inventarioId, Pageable pageable);
}
