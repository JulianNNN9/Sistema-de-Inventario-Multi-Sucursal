package com.optiplant.inventario.transferencia.repository;

import com.optiplant.inventario.transferencia.entity.Transportista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Transportista> findAllByOrderByNombreAsc(Pageable pageable);
}
