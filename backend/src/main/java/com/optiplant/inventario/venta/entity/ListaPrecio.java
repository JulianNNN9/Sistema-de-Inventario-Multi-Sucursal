package com.optiplant.inventario.venta.entity;

import com.optiplant.inventario.sucursal.entity.Sucursal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de precios (RF-15). {@code sucursal} nula = lista global aplicable a
 * toda la red.
 */
@Entity
@Table(name = "lista_precio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Builder.Default
    @OneToMany(mappedBy = "lista", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ListaPrecioDetalle> detalles = new ArrayList<>();

    public void addDetalle(ListaPrecioDetalle detalle) {
        detalles.add(detalle);
        detalle.setLista(this);
    }

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
