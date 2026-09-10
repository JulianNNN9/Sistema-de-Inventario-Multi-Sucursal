package com.optiplant.inventario.producto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/**
 * Unidad de medida alternativa de un producto (RF-06). {@code factorConversion}
 * indica cuántas unidades base equivalen a 1 de esta unidad.
 */
@Entity
@Table(name = "producto_unidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoUnidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre_unidad", nullable = false, length = 30)
    private String nombreUnidad;

    @Column(name = "factor_conversion", nullable = false, precision = 12, scale = 4)
    private BigDecimal factorConversion;
}
