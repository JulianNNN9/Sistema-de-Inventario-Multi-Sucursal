package com.optiplant.inventario.usuario.repository;

import com.optiplant.inventario.usuario.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Trae la sucursal en la misma consulta para evitar N+1 al listar (RNF-01). */
    @Override
    @EntityGraph(attributePaths = "sucursal")
    Page<Usuario> findAll(Pageable pageable);
}
