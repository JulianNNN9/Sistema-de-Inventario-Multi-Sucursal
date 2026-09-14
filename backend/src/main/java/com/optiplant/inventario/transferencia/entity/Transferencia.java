package com.optiplant.inventario.transferencia.entity;

import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.sucursal.entity.Sucursal;
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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transferencia de un producto entre dos sucursales (RF-17..RF-21). La
 * confirmación de recepción (total o parcial) sólo la hace la sucursal destino
 * (RN-02). Inmutable salvo transición de estado (regla global 6).
 */
@Entity
@Table(name = "transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_origen_id", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_destino_id", nullable = false)
    private Sucursal sucursalDestino;

    @Column(name = "cantidad_solicitada", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidadSolicitada;

    @Column(name = "cantidad_enviada", precision = 12, scale = 2)
    private BigDecimal cantidadEnviada;

    @Column(name = "cantidad_recibida", precision = 12, scale = 2)
    private BigDecimal cantidadRecibida;

    /**
     * Costo real del envío (RF-23), capturado en el despacho ({@code DispatchRequest});
     * en la solicitud aún no se conoce, por eso el default 0 hasta que se despache.
     */
    @Builder.Default
    @Column(name = "costo", nullable = false, precision = 12, scale = 2)
    private BigDecimal costo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTransferencia estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Urgencia urgencia;

    @Column(length = 120)
    private String transportista;

    @Column(name = "fecha_estimada_llegada")
    private Instant fechaEstimadaLlegada;

    @Column(name = "fecha_real_llegada")
    private Instant fechaRealLlegada;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
