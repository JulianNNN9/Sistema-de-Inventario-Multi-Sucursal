package com.optiplant.inventario.compra.repository;

import com.optiplant.inventario.compra.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
