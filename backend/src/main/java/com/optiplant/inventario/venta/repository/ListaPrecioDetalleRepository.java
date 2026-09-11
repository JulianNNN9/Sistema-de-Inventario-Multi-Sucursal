package com.optiplant.inventario.venta.repository;

import com.optiplant.inventario.venta.entity.ListaPrecioDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListaPrecioDetalleRepository extends JpaRepository<ListaPrecioDetalle, Long> {

    Optional<ListaPrecioDetalle> findByListaIdAndProductoId(Long listaId, Long productoId);

    boolean existsByListaIdAndProductoId(Long listaId, Long productoId);
}
