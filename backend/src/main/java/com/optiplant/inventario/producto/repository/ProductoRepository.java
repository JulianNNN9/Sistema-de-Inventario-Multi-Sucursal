package com.optiplant.inventario.producto.repository;

import com.optiplant.inventario.producto.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Catálogo visible para una sucursal: productos que tienen existencias
     * registradas en ella (RF-01). Agregación en BD, paginada (RNF-01).
     * {@code search} filtra por SKU o nombre (contiene, sin distinguir mayúsculas);
     * se ignora si es nulo o vacío.
     */
    @Query("""
            select p from Producto p
            where exists (
                select 1 from InventarioSucursal i
                where i.producto = p and i.sucursal.id = :sucursalId
            )
            and (:search is null or :search = '' or lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(p.nombre) like lower(concat('%', :search, '%')))
            """)
    Page<Producto> findAllInSucursal(@Param("sucursalId") Long sucursalId, @Param("search") String search,
                                     Pageable pageable);

    /** Catálogo completo (ADMIN_GENERAL sin sucursal), con el mismo filtro de búsqueda. */
    @Query("""
            select p from Producto p
            where (:search is null or :search = '' or lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(p.nombre) like lower(concat('%', :search, '%')))
            """)
    Page<Producto> buscar(@Param("search") String search, Pageable pageable);
}
