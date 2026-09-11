package com.optiplant.inventario.venta.repository;

import com.optiplant.inventario.venta.entity.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

    List<VentaDetalle> findByVentaId(Long ventaId);
}
