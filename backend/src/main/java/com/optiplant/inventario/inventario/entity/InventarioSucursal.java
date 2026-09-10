package com.optiplant.inventario.inventario.entity;

import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Existencias de un producto en una sucursal (Sección 3). Una única fila por
 * par (producto, sucursal). {@code costoPromedioPonderado} se recalcula al
 * recibir compras (RF-12).
 */
@Entity
@Table(
        name = "inventario_sucursal",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventario_producto_sucursal",
                columnNames = {"producto_id", "sucursal_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioSucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Builder.Default
    @Column(name = "cantidad_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidadActual = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "costo_promedio_ponderado", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoPromedioPonderado = BigDecimal.ZERO;
}
