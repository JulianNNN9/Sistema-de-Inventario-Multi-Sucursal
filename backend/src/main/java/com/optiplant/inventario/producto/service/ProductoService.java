package com.optiplant.inventario.producto.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.ConflictoEstadoException;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.producto.dto.ProductoRequest;
import com.optiplant.inventario.producto.dto.ProductoResponse;
import com.optiplant.inventario.producto.dto.ProductoUnidadRequest;
import com.optiplant.inventario.producto.dto.ProductoUnidadResponse;
import com.optiplant.inventario.producto.dto.ProductoUpdateRequest;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.entity.ProductoUnidad;
import com.optiplant.inventario.producto.repository.ProductoRepository;
import com.optiplant.inventario.producto.repository.ProductoUnidadRepository;
import com.optiplant.inventario.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio del catálogo de productos y sus unidades de medida
 * (RF-01, RF-06). El {@code producto} es global a la red (SUP-03); el listado
 * {@code GET /products} se acota a la sucursal del usuario (Sección 4.2).
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoUnidadRepository productoUnidadRepository;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final CurrentUser currentUser;

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsBySku(request.sku())) {
            throw new ValidacionException("campo 'sku': ya existe un producto con ese sku");
        }
        Producto producto = Producto.builder()
                .sku(request.sku())
                .nombre(request.nombre())
                .unidadMedidaBase(request.unidadMedidaBase())
                .build();
        return toResponse(productoRepository.save(producto));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return toResponse(getEntityById(id));
    }

    /**
     * {@code branchIdParam} sólo se respeta para ADMIN_GENERAL; el resto de roles
     * ve siempre el catálogo de su propia sucursal (Sección 4.2, RF-01).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listar(Long branchIdParam, Pageable pageable) {
        Long effectiveBranchId = currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
        Page<Producto> page = effectiveBranchId == null
                ? productoRepository.findAll(pageable)
                : productoRepository.findAllInSucursal(effectiveBranchId, pageable);
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
        productoUnidadRepository.deleteByProductoId(id);
        productoRepository.delete(producto);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoUnidadResponse> listarUnidades(Long productoId, Pageable pageable) {
        if (!productoRepository.existsById(productoId)) {
            throw new RecursoNoEncontradoException("Producto", productoId);
        }
        return PageResponse.from(
                productoUnidadRepository.findByProductoId(productoId, pageable).map(this::toUnidadResponse));
    }

    @Transactional
    public ProductoUnidadResponse agregarUnidad(Long productoId, ProductoUnidadRequest request) {
        Producto producto = getEntityById(productoId);
        if (productoUnidadRepository.existsByProductoIdAndNombreUnidad(productoId, request.nombreUnidad())) {
            throw new ValidacionException(
                    "campo 'nombreUnidad': el producto ya tiene una unidad de medida con ese nombre");
        }
        ProductoUnidad unidad = ProductoUnidad.builder()
                .producto(producto)
                .nombreUnidad(request.nombreUnidad())
                .factorConversion(request.factorConversion())
                .build();
        return toUnidadResponse(productoUnidadRepository.save(unidad));
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

    private ProductoUnidadResponse toUnidadResponse(ProductoUnidad unidad) {
        return new ProductoUnidadResponse(
                unidad.getId(), unidad.getProducto().getId(),
                unidad.getNombreUnidad(), unidad.getFactorConversion());
    }
}
