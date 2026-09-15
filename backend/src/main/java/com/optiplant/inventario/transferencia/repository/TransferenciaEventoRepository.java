package com.optiplant.inventario.transferencia.repository;

import com.optiplant.inventario.transferencia.entity.TransferenciaEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    /**
     * Variante en lote de {@link #existsByTransferenciaIdAndComentario} para no
     * hacer una consulta por fila al listar (RNF-01): trae los id de transferencia,
     * dentro de {@code transferenciaIds}, que ya tienen ese evento de aprobación.
     */
    @Query("""
            select distinct te.transferencia.id from TransferenciaEvento te
            where te.transferencia.id in :transferenciaIds and te.comentario = :comentario
            """)
    List<Long> findTransferenciaIdsConComentario(@Param("transferenciaIds") List<Long> transferenciaIds,
                                                  @Param("comentario") String comentario);
}
