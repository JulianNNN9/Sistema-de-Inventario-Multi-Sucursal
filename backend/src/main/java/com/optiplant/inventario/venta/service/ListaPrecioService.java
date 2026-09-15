package com.optiplant.inventario.venta.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.service.ProductoService;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.venta.dto.PriceListItemRequest;
import com.optiplant.inventario.venta.dto.PriceListItemResponse;
import com.optiplant.inventario.venta.dto.PriceListRequest;
import com.optiplant.inventario.venta.dto.PriceListResponse;
import com.optiplant.inventario.venta.dto.PriceListUpdateRequest;
import com.optiplant.inventario.venta.entity.ListaPrecio;
import com.optiplant.inventario.venta.entity.ListaPrecioDetalle;
import com.optiplant.inventario.venta.repository.ListaPrecioDetalleRepository;
import com.optiplant.inventario.venta.repository.ListaPrecioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Listas de precios (RF-15). Una lista sin sucursal es global; el resto de roles
 * (no ADMIN) sólo crea listas de su propia sucursal.
 */
@Service
@RequiredArgsConstructor
public class ListaPrecioService {

    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioDetalleRepository listaPrecioDetalleRepository;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final CurrentUser currentUser;

    @Transactional
    public PriceListResponse crear(PriceListRequest request) {
        Sucursal sucursal = resolverSucursal(request.branchId());

        ListaPrecio lista = ListaPrecio.builder()
                .nombre(request.nombre())
                .sucursal(sucursal)
                .build();

        Set<Long> productosVistos = new HashSet<>();
        for (PriceListItemRequest item : request.items()) {
            Producto producto = productoService.getEntityById(item.productId());
            if (!productosVistos.add(item.productId())) {
                throw new ValidacionException(
                        "El producto \"" + producto.getNombre() + "\" está repetido en la lista");
            }
            lista.addDetalle(ListaPrecioDetalle.builder()
                    .producto(producto)
                    .precio(item.precio())
                    .build());
        }

        return toResponse(listaPrecioRepository.save(lista));
    }

    @Transactional(readOnly = true)
    public PageResponse<PriceListResponse> listar(Pageable pageable) {
        Long scope = currentUser.isAdmin() ? null : currentUser.sucursalId();
        return PageResponse.from(listaPrecioRepository.findVisibles(scope, pageable).map(this::toResponse));
    }

    /**
     * No-admin solo modifica listas de su propia sucursal (igual que al crear);
     * las globales (RF-15) son política de red y quedan reservadas a ADMIN_GENERAL.
     */
    @Transactional
    public PriceListResponse actualizar(Long id, PriceListUpdateRequest request) {
        ListaPrecio lista = getEntityById(id);
        assertPuedeGestionar(lista);

        lista.setNombre(request.nombre());

        listaPrecioDetalleRepository.deleteAll(lista.getDetalles());
        lista.getDetalles().clear();

        Set<Long> productosVistos = new HashSet<>();
        for (PriceListItemRequest item : request.items()) {
            Producto producto = productoService.getEntityById(item.productId());
            if (!productosVistos.add(item.productId())) {
                throw new ValidacionException(
                        "El producto \"" + producto.getNombre() + "\" está repetido en la lista");
            }
            lista.addDetalle(ListaPrecioDetalle.builder()
                    .producto(producto)
                    .precio(item.precio())
                    .build());
        }

        return toResponse(listaPrecioRepository.save(lista));
    }

    private ListaPrecio getEntityById(Long id) {
        return listaPrecioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lista de precios", id));
    }

    private void assertPuedeGestionar(ListaPrecio lista) {
        if (currentUser.isAdmin()) {
            return;
        }
        Sucursal sucursal = lista.getSucursal();
        if (sucursal == null || !sucursal.getId().equals(currentUser.sucursalId())) {
            throw new AccessDeniedException("No puede modificar esta lista de precios");
        }
    }

    /** Precio de un producto en una lista (RF-15). Usado por {@code VentaService}. */
    @Transactional(readOnly = true)
    public BigDecimal resolverPrecio(Long priceListId, Long productId) {
        ListaPrecio lista = listaPrecioRepository.findById(priceListId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lista de precios", priceListId));
        return listaPrecioDetalleRepository.findByListaIdAndProductoId(priceListId, productId)
                .map(ListaPrecioDetalle::getPrecio)
                .orElseThrow(() -> new ValidacionException(
                        "La lista de precios \"" + lista.getNombre() + "\" no tiene un precio definido para uno de los productos seleccionados"));
    }

    private Sucursal resolverSucursal(Long branchIdFromRequest) {
        if (currentUser.isAdmin()) {
            return branchIdFromRequest == null
                    ? null
                    : sucursalService.getEntityById(branchIdFromRequest);
        }
        Long propia = currentUser.sucursalId();
        if (branchIdFromRequest != null && !branchIdFromRequest.equals(propia)) {
            throw new AccessDeniedException("Solo puede crear listas de precios de su propia sucursal");
        }
        return sucursalService.getEntityById(propia);
    }

    private PriceListResponse toResponse(ListaPrecio lista) {
        List<PriceListItemResponse> items = lista.getDetalles().stream()
                .map(d -> new PriceListItemResponse(
                        d.getProducto().getId(),
                        d.getProducto().getSku(),
                        d.getProducto().getNombre(),
                        d.getPrecio()))
                .toList();
        Sucursal sucursal = lista.getSucursal();
        return new PriceListResponse(
                lista.getId(),
                lista.getNombre(),
                sucursal != null ? sucursal.getId() : null,
                sucursal != null ? sucursal.getNombre() : null,
                items);
    }
}
