package com.optiplant.inventario.compra.repository;

import com.optiplant.inventario.compra.entity.OrdenCompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenCompraDetalleRepository extends JpaRepository<OrdenCompraDetalle, Long> {

    List<OrdenCompraDetalle> findByOrdenId(Long ordenId);
}
