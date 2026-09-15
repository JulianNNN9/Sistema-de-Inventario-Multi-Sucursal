package com.optiplant.inventario.compra.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.ConflictoEstadoException;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.compra.dto.PurchaseOrderLineRequest;
import com.optiplant.inventario.compra.dto.PurchaseOrderLineResponse;
import com.optiplant.inventario.compra.dto.PurchaseOrderRequest;
import com.optiplant.inventario.compra.dto.PurchaseOrderResponse;
import com.optiplant.inventario.compra.dto.PurchaseOrderSummaryResponse;
import com.optiplant.inventario.compra.entity.EstadoOrdenCompra;
import com.optiplant.inventario.compra.entity.OrdenCompra;
import com.optiplant.inventario.compra.entity.OrdenCompraDetalle;
import com.optiplant.inventario.compra.entity.Proveedor;
import com.optiplant.inventario.compra.repository.OrdenCompraRepository;
import com.optiplant.inventario.inventario.service.InventarioService;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.service.ProductoService;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Reglas de negocio de compras (RF-08..RF-12). {@code confirmarRecepcion} es una
 * operación tipo Facade: orquesta órdenes, inventario y movimientos en una única
 * transacción (Sección 6).
 */
@Service
@RequiredArgsConstructor
public class CompraService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final InventarioService inventarioService;
    private final CurrentUser currentUser;

    @Transactional
    public PurchaseOrderResponse crear(PurchaseOrderRequest request) {
        Long branchId = resolverSucursal(request.branchId());
        currentUser.assertPuedeOperarSobreSucursal(branchId);

        Proveedor proveedor = proveedorService.getEntityById(request.supplierId());
        Sucursal sucursal = sucursalService.getEntityById(branchId);

        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .sucursal(sucursal)
                .fecha(Instant.now())
                .estado(EstadoOrdenCompra.PENDIENTE)
                .plazoPago(proveedor.getFrecuenciaPago())
                .build();

        for (PurchaseOrderLineRequest linea : request.lineas()) {
            Producto producto = productoService.getEntityById(linea.productId());
            orden.addDetalle(OrdenCompraDetalle.builder()
                    .producto(producto)
                    .cantidad(linea.cantidad())
                    .precioUnitario(linea.precioUnitario())
                    .descuento(linea.descuento() != null ? linea.descuento() : BigDecimal.ZERO)
                    .build());
        }

        return toResponse(ordenCompraRepository.save(orden));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse obtener(Long id) {
        OrdenCompra orden = cargarConAcceso(id);
        return toResponse(orden);
    }

    /** {@code soloActivas}: true = solo PENDIENTES (vista principal); false = histórico (RECIBIDA/CANCELADA). */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderSummaryResponse> listar(Long supplierId, Long productId,
                                                             Long branchIdParam, boolean soloActivas,
                                                             Pageable pageable) {
        Long branchId = currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
        return PageResponse.from(
                ordenCompraRepository.searchSummaries(supplierId, branchId, productId, soloActivas, pageable));
    }

    /**
     * Worklist de OPERADOR_INVENTARIO: solo las órdenes PENDIENTES de su propia
     * sucursal, para saber qué confirmar. No es el histórico (sin filtros por
     * proveedor/producto, sin otros estados).
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderSummaryResponse> listarPendientesPropios(Pageable pageable) {
        return PageResponse.from(ordenCompraRepository.findPendingByBranch(
                currentUser.sucursalId(), EstadoOrdenCompra.PENDIENTE, pageable));
    }

    /**
     * Facade — RF-10 / RF-12. Por cada línea: ingreso de stock + recálculo del
     * costo promedio ponderado + movimiento de inventario; luego la orden pasa a
     * RECIBIDA. Todo en la misma transacción.
     */
    @Transactional
    public PurchaseOrderResponse confirmarRecepcion(Long id) {
        OrdenCompra orden = cargarConAcceso(id);
        currentUser.assertPuedeOperarSobreSucursal(orden.getSucursal().getId());

        if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE) {
            throw new ConflictoEstadoException(
                    "La orden de compra está en estado " + orden.getEstado()
                            + " y no puede confirmarse de nuevo");
        }

        Long responsableId = currentUser.usuarioId();
        for (OrdenCompraDetalle detalle : orden.getDetalles()) {
            BigDecimal costoNeto = subtotalNeto(detalle.getCantidad(), detalle.getPrecioUnitario(), detalle.getDescuento());
            BigDecimal precioUnitarioNeto = costoNeto.divide(detalle.getCantidad(), 2, RoundingMode.HALF_UP);
            inventarioService.registrarIngresoPorCompra(
                    detalle.getProducto(),
                    orden.getSucursal(),
                    detalle.getCantidad(),
                    precioUnitarioNeto,
                    responsableId);
        }

        orden.setEstado(EstadoOrdenCompra.RECIBIDA);
        return toResponse(ordenCompraRepository.save(orden));
    }

    /** Solo se puede cancelar una orden PENDIENTE: una RECIBIDA ya movió inventario, una CANCELADA no se repite. */
    @Transactional
    public PurchaseOrderResponse cancelar(Long id) {
        OrdenCompra orden = cargarConAcceso(id);
        currentUser.assertPuedeOperarSobreSucursal(orden.getSucursal().getId());

        if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE) {
            throw new ConflictoEstadoException(
                    "La orden de compra está en estado " + orden.getEstado() + " y no puede cancelarse");
        }

        orden.setEstado(EstadoOrdenCompra.CANCELADA);
        return toResponse(ordenCompraRepository.save(orden));
    }

    private OrdenCompra cargarConAcceso(Long id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de compra", id));
        if (!currentUser.isAdmin()
                && !Objects.equals(orden.getSucursal().getId(), currentUser.sucursalId())) {
            throw new RecursoNoEncontradoException("Orden de compra", id);
        }
        return orden;
    }

    private Long resolverSucursal(Long branchIdFromRequest) {
        if (currentUser.isAdmin()) {
            if (branchIdFromRequest == null) {
                throw new ValidacionException("Debes seleccionar la sucursal para la que se registra la orden");
            }
            return branchIdFromRequest;
        }
        Long propia = currentUser.sucursalId();
        if (branchIdFromRequest != null && !branchIdFromRequest.equals(propia)) {
            throw new AccessDeniedException("No puede registrar compras de otra sucursal");
        }
        return propia;
    }

    /** Subtotal neto de una línea: {@code cantidad * precioUnitario * (1 - descuento / 100)}. */
    private BigDecimal subtotalNeto(BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal descuento) {
        BigDecimal bruto = cantidad.multiply(precioUnitario);
        BigDecimal factor = BigDecimal.ONE.subtract(descuento.divide(CIEN, 6, RoundingMode.HALF_UP));
        return bruto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private PurchaseOrderResponse toResponse(OrdenCompra orden) {
        List<PurchaseOrderLineResponse> lineas = orden.getDetalles().stream()
                .map(d -> new PurchaseOrderLineResponse(
                        d.getProducto().getId(),
                        d.getProducto().getSku(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getDescuento(),
                        subtotalNeto(d.getCantidad(), d.getPrecioUnitario(), d.getDescuento())))
                .toList();

        BigDecimal total = lineas.stream()
                .map(PurchaseOrderLineResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseOrderResponse(
                orden.getId(),
                orden.getProveedor().getId(),
                orden.getProveedor().getNombre(),
                orden.getSucursal().getId(),
                orden.getSucursal().getNombre(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getPlazoPago(),
                total,
                lineas);
    }
}
