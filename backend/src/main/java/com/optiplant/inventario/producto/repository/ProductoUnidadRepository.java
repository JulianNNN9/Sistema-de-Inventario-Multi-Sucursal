package com.optiplant.inventario.producto.repository;

import com.optiplant.inventario.producto.entity.ProductoUnidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoUnidadRepository extends JpaRepository<ProductoUnidad, Long> {

    List<ProductoUnidad> findByProductoId(Long productoId);

    Page<ProductoUnidad> findByProductoId(Long productoId, Pageable pageable);

    boolean existsByProductoIdAndNombreUnidad(Long productoId, String nombreUnidad);

    void deleteByProductoId(Long productoId);
}
