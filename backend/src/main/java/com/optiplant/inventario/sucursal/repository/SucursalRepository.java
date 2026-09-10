package com.optiplant.inventario.sucursal.repository;

import com.optiplant.inventario.sucursal.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
}
