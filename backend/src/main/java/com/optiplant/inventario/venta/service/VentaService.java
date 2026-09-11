package com.optiplant.inventario.venta.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.StockInsuficienteException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.inventario.service.InventarioService;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.service.ProductoService;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import com.optiplant.inventario.venta.dto.SaleLineRequest;
import com.optiplant.inventario.venta.dto.SaleLineResponse;
import com.optiplant.inventario.venta.dto.SaleRequest;
import com.optiplant.inventario.venta.dto.SaleResponse;
import com.optiplant.inventario.venta.dto.SaleSummaryResponse;
import com.optiplant.inventario.venta.entity.Venta;
import com.optiplant.inventario.venta.entity.VentaDetalle;
import com.optiplant.inventario.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reglas de negocio de ventas (RF-13..RF-16). {@code crear} es una operación
 * tipo Facade: valida stock, crea la venta y descuenta inventario en una única
 * transacción; si cualquier línea falla, no se persiste nada (RN-01).
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final VentaRepository ventaRepository;
    private final ListaPrecioService listaPrecioService;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final InventarioService inventarioService;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUser currentUser;

    private record LineaResuelta(Producto producto, BigDecimal cantidad,
                                 BigDecimal precioUnitario, BigDecimal descuento) {
    }

    @Transactional
    public SaleResponse crear(SaleRequest request) {
        Long branchId = resolverSucursal(request.branchId());
        currentUser.assertPuedeOperarSobreSucursal(branchId);
        Sucursal sucursal = sucursalService.getEntityById(branchId);

        List<LineaResuelta> resueltas = new ArrayList<>();
        Map<Long, BigDecimal> requeridoPorProducto = new HashMap<>();
        for (SaleLineRequest linea : request.lineas()) {
            Producto producto = productoService.getEntityById(linea.productId());
            BigDecimal precio = linea.priceListId() != null
                    ? listaPrecioService.resolverPrecio(linea.priceListId(), linea.productId())
                    : requerido(linea.precioUnitario(),
                            "campo 'precioUnitario': es obligatorio cuando la línea no referencia priceListId");
            BigDecimal descuento = linea.descuento() != null ? linea.descuento() : BigDecimal.ZERO;
            resueltas.add(new LineaResuelta(producto, linea.cantidad(), precio, descuento));
            requeridoPorProducto.merge(producto.getId(), linea.cantidad(), BigDecimal::add);
        }

        // RF-14 / RN-01: validar stock (agregado por producto) ANTES de persistir nada.
        for (Map.Entry<Long, BigDecimal> requerido : requeridoPorProducto.entrySet()) {
            BigDecimal disponible = inventarioSucursalRepository
                    .findByProductoIdAndSucursalId(requerido.getKey(), branchId)
                    .map(InventarioSucursal::getCantidadActual)
                    .orElse(BigDecimal.ZERO);
            if (disponible.compareTo(requerido.getValue()) < 0) {
                throw new StockInsuficienteException();
            }
        }

        Usuario responsable = usuarioRepository.findById(currentUser.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", currentUser.usuarioId()));

        Venta venta = Venta.builder()
                .sucursal(sucursal)
                .usuario(responsable)
                .fecha(Instant.now())
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (LineaResuelta r : resueltas) {
            total = total.add(subtotalNeto(r.cantidad(), r.precioUnitario(), r.descuento()));
            venta.addDetalle(VentaDetalle.builder()
                    .producto(r.producto())
                    .cantidad(r.cantidad())
                    .precioUnitario(r.precioUnitario())
                    .descuento(r.descuento())
                    .build());
        }
        venta.setTotal(total);
        Venta guardada = ventaRepository.save(venta);

        for (LineaResuelta r : resueltas) {
            inventarioService.registrarSalidaPorVenta(r.producto(), sucursal, r.cantidad(), responsable.getId());
        }

        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public SaleResponse obtener(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
        if (!currentUser.isAdmin()
                && !Objects.equals(venta.getSucursal().getId(), currentUser.sucursalId())) {
            throw new RecursoNoEncontradoException("Venta", id);
        }
        return toResponse(venta);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> listar(Long branchIdParam, LocalDate from, LocalDate to,
                                                    Pageable pageable) {
        Long branchId = currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
        // Centinelas de rango cuando el filtro no viene (evita bind timestamptz nulo en JPQL).
        Instant desde = from != null
                ? from.atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.EPOCH;
        Instant hasta = to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().plusSeconds(86_400);
        return PageResponse.from(
                ventaRepository.search(branchId, desde, hasta, pageable).map(this::toSummary));
    }

    private Long resolverSucursal(Long branchIdFromRequest) {
        if (currentUser.isAdmin()) {
            if (branchIdFromRequest == null) {
                throw new ValidacionException("campo 'branchId': es obligatorio para ADMIN_GENERAL");
            }
            return branchIdFromRequest;
        }
        Long propia = currentUser.sucursalId();
        if (branchIdFromRequest != null && !branchIdFromRequest.equals(propia)) {
            throw new AccessDeniedException("No puede registrar ventas de otra sucursal");
        }
        return propia;
    }

    private BigDecimal requerido(BigDecimal value, String message) {
        if (value == null) {
            throw new ValidacionException(message);
        }
        return value;
    }

    private BigDecimal subtotalNeto(BigDecimal cantidad, BigDecimal precio, BigDecimal descuento) {
        BigDecimal bruto = cantidad.multiply(precio);
        BigDecimal factor = BigDecimal.ONE.subtract(descuento.divide(CIEN, 6, RoundingMode.HALF_UP));
        return bruto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private SaleResponse toResponse(Venta venta) {
        List<SaleLineResponse> lineas = venta.getDetalles().stream()
                .map(d -> new SaleLineResponse(
                        d.getProducto().getId(),
                        d.getProducto().getSku(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getDescuento(),
                        subtotalNeto(d.getCantidad(), d.getPrecioUnitario(), d.getDescuento())))
                .toList();
        return new SaleResponse(
                venta.getId(),
                venta.getSucursal().getId(),
                venta.getSucursal().getNombre(),
                venta.getUsuario().getId(),
                venta.getUsuario().getNombre(),
                venta.getFecha(),
                venta.getTotal(),
                lineas);
    }

    private SaleSummaryResponse toSummary(Venta venta) {
        return new SaleSummaryResponse(
                venta.getId(),
                venta.getSucursal().getId(),
                venta.getSucursal().getNombre(),
                venta.getUsuario().getNombre(),
                venta.getFecha(),
                venta.getTotal());
    }
}
