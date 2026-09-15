package com.optiplant.inventario.producto.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.ConflictoEstadoException;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.producto.dto.ProductoRequest;
import com.optiplant.inventario.producto.dto.ProductoResponse;
import com.optiplant.inventario.producto.dto.ProductoUpdateRequest;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.repository.ProductoRepository;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio del catálogo de productos (RF-01). El {@code producto} es
 * global a la red (SUP-03); el listado {@code GET /products} se acota a la
 * sucursal del usuario (Sección 4.2).
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final SucursalRepository sucursalRepository;
    private final CurrentUser currentUser;

    /**
     * El catálogo visible por sucursal ({@link #listar}) se resuelve por
     * existencia de {@link InventarioSucursal}, así que un producto creado por
     * un usuario no-ADMIN debe quedar con una fila de existencias (en 0) en su
     * propia sucursal; de lo contrario nunca aparecería en su propio listado.
     */
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsBySku(request.sku())) {
            throw new ValidacionException("Ya existe un producto con ese SKU");
        }
        Producto producto = Producto.builder()
                .sku(request.sku())
                .nombre(request.nombre())
                .unidadMedidaBase(request.unidadMedidaBase())
                .build();
        producto = productoRepository.save(producto);

        if (!currentUser.isAdmin()) {
            inventarioSucursalRepository.save(InventarioSucursal.builder()
                    .producto(producto)
                    .sucursal(sucursalRepository.getReferenceById(currentUser.sucursalId()))
                    .build());
        }

        return toResponse(producto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return toResponse(getEntityById(id));
    }

    /**
     * {@code branchIdParam} sólo se respeta para ADMIN_GENERAL; el resto de roles
     * ve siempre el catálogo de su propia sucursal (Sección 4.2, RF-01).
     * {@code search} filtra por SKU o nombre (contiene, sin distinguir mayúsculas).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listar(Long branchIdParam, String search, Pageable pageable) {
        Long effectiveBranchId = currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
        Page<Producto> page = effectiveBranchId == null
                ? productoRepository.buscar(search, pageable)
                : productoRepository.findAllInSucursal(effectiveBranchId, search, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest request) {
        Producto producto = getEntityById(id);
        producto.setNombre(request.nombre());
        producto.setUnidadMedidaBase(request.unidadMedidaBase());
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = getEntityById(id);
        if (inventarioSucursalRepository.existsByProductoId(id)) {
            throw new ConflictoEstadoException(
                    "No se puede eliminar el producto porque tiene inventario asociado en una o más sucursales");
        }
        productoRepository.delete(producto);
    }

    /** Reutilizado por otros módulos para resolver referencias a producto. */
    @Transactional(readOnly = true)
    public Producto getEntityById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

    private ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(), producto.getSku(), producto.getNombre(),
                producto.getUnidadMedidaBase());
    }
}
