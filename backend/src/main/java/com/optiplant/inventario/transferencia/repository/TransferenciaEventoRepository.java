package com.optiplant.inventario.transferencia.repository;

import com.optiplant.inventario.transferencia.entity.TransferenciaEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaEventoRepository extends JpaRepository<TransferenciaEvento, Long> {

    /** Histórico de una transferencia (RF-22). Paginado; ordenar por fecha en el endpoint. */
    Page<TransferenciaEvento> findByTransferenciaId(Long transferenciaId, Pageable pageable);

    /**
     * El modelo de estados (Sección 3) no tiene un estado "APROBADA" propio: la
     * aprobación del origen deja la transferencia en {@code PENDIENTE} ("continúa
     * disponible para preparación"). Se registra como evento con un comentario
     * estable y se consulta así para exigir la aprobación previa al despacho.
     */
    boolean existsByTransferenciaIdAndComentario(Long transferenciaId, String comentario);
}
