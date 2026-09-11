package com.optiplant.inventario.transferencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro append-only de un cambio de estado de una transferencia
 * (RF-22, RF-24). Inmutable (regla global 6).
 */
@Entity
@Table(name = "transferencia_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferenciaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferencia_id", nullable = false)
    private Transferencia transferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTransferencia estado;

    @Column(nullable = false)
    private Instant fecha;

    @Column(columnDefinition = "text")
    private String comentario;
}
