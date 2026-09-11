package com.optiplant.inventario.compra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Proveedor de mercancía (Sección 3, RF-08). */
@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    /** Cada cuánto se le paga al proveedor (ej. "30 días", "Contado"). Obligatorio. */
    @Column(name = "frecuencia_pago", nullable = false, length = 60)
    private String frecuenciaPago;

    @Column(columnDefinition = "text")
    private String condiciones;
}
