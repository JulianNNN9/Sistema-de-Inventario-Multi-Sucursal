package com.optiplant.inventario.inventario.repository;

import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventarioSucursalRepository extends JpaRepository<InventarioSucursal, Long> {

    Optional<InventarioSucursal> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    boolean existsByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    boolean existsByProductoId(Long productoId);

    /**
     * Inventario de una sucursal, paginado, con producto y sucursal ya cargados (RNF-01).
     * {@code search} filtra por SKU o nombre de producto (contiene, sin distinguir
     * mayúsculas); {@code soloBajoMinimo} restringe a filas en o bajo su stock mínimo.
     */
    @EntityGraph(attributePaths = {"producto", "sucursal"})
    @Query("""
            select inv from InventarioSucursal inv
            where inv.sucursal.id = :sucursalId
              and (:search is null or :search = '' or lower(inv.producto.sku) like lower(concat('%', :search, '%'))
                  or lower(inv.producto.nombre) like lower(concat('%', :search, '%')))
              and (:soloBajoMinimo = false or inv.cantidadActual <= inv.stockMinimo)
            """)
    Page<InventarioSucursal> findBySucursalId(@Param("sucursalId") Long sucursalId,
                                              @Param("search") String search,
                                              @Param("soloBajoMinimo") boolean soloBajoMinimo,
                                              Pageable pageable);

    /**
     * RF-29: productos cuya existencia ya alcanzó (o bajó) el mínimo. Sin
     * paginar por diseño (reporte acotado por catálogo de la sucursal, igual
     * criterio que el reporte de cumplimiento del Módulo 5).
     */
    @EntityGraph(attributePaths = {"producto", "sucursal"})
    @Query("""
            select inv from InventarioSucursal inv
            where inv.cantidadActual <= inv.stockMinimo
              and (:branchId is null or inv.sucursal.id = :branchId)
            order by inv.sucursal.nombre, inv.producto.nombre
            """)
    List<InventarioSucursal> restockAlerts(@Param("branchId") Long branchId);

    /**
     * RF-31 (Módulo 7): única prefiltración que se delega a BD — reduce el
     * conjunto a las filas realmente candidatas (déficit o superávit); el
     * emparejamiento en sí es un algoritmo (Strategy), no una agregación SQL,
     * así que no aplica la prohibición de RNF-01 sobre sumar en Java.
     */
    @EntityGraph(attributePaths = {"producto", "sucursal"})
    @Query("""
            select inv from InventarioSucursal inv
            where inv.cantidadActual < inv.stockMinimo
               or inv.cantidadActual > inv.stockMinimo * :umbralSuperavit
            order by inv.producto.id, inv.sucursal.id
            """)
    List<InventarioSucursal> findCandidatosRebalanceo(@Param("umbralSuperavit") BigDecimal umbralSuperavit);
}
