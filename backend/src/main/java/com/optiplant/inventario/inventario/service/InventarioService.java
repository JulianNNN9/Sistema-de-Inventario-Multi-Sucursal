package com.optiplant.inventario.inventario.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.StockInsuficienteException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.inventario.dto.InventarioResponse;
import com.optiplant.inventario.inventario.dto.MinStockRequest;
import com.optiplant.inventario.inventario.dto.MovimientoRequest;
import com.optiplant.inventario.inventario.dto.MovimientoResponse;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.entity.MotivoMovimiento;
import com.optiplant.inventario.inventario.entity.MovimientoInventario;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.inventario.repository.MovimientoInventarioRepository;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.repository.ProductoRepository;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Reglas de negocio de existencias y movimientos de inventario
 * (RF-02..RF-05, RF-07). Todo movimiento actualiza {@code cantidad_actual} en la
 * misma transacción; los retiros validan stock suficiente (RN-01).
 */
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioSucursalRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final SucursalService sucursalService;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUser currentUser;

    @Transactional
    public MovimientoResponse registrarMovimiento(MovimientoRequest request) {
        if (request.motivo() == MotivoMovimiento.TRANSFERENCIA_SALIDA
                || request.motivo() == MotivoMovimiento.TRANSFERENCIA_ENTRADA) {
            throw new ValidacionException(
                    "campo 'motivo': TRANSFERENCIA_SALIDA y TRANSFERENCIA_ENTRADA se generan solo desde el módulo de transferencias");
        }
        currentUser.assertPuedeOperarSobreSucursal(request.branchId());

        Producto producto = productoRepository.findById(request.productId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", request.productId()));
        Sucursal sucursal = sucursalService.getEntityById(request.branchId());
        InventarioSucursal inventario = getOrCreateInventario(producto, sucursal);

        BigDecimal cantidad = request.cantidad();
        if (request.tipo() == TipoMovimiento.RETIRO) {
            if (inventario.getCantidadActual().compareTo(cantidad) < 0) {
                throw new StockInsuficienteException();
            }
            inventario.setCantidadActual(inventario.getCantidadActual().subtract(cantidad));
        } else {
            inventario.setCantidadActual(inventario.getCantidadActual().add(cantidad));
        }
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = movimientoRepository.save(MovimientoInventario.builder()
                .inventario(inventario)
                .tipo(request.tipo())
                .motivo(request.motivo())
                .cantidad(cantidad)
                .fecha(Instant.now())
                .responsable(usuarioRepository.getReferenceById(currentUser.usuarioId()))
                .build());

        return new MovimientoResponse(
                movimiento.getId(),
                producto.getId(),
                sucursal.getId(),
                request.tipo(),
                request.motivo(),
                cantidad,
                inventario.getCantidadActual(),
                inventario.getStockMinimo(),
                estaBajoMinimo(inventario),
                movimiento.getFecha());
    }

    /**
     * Aplica el ingreso de una línea de compra recibida (RF-10): incrementa la
     * existencia, recalcula el costo promedio ponderado (RF-12) y registra el
     * {@code MovimientoInventario} (INGRESO / COMPRA). Pensado para orquestarse
     * desde {@code CompraService.confirmarRecepcion} dentro de su transacción.
     *
     * <p>Fórmula (Sección 8.2 / Módulo 2):
     * {@code nuevo_costo = (qty_previa * costo_previo + qty_recibida * precio_unitario) / (qty_previa + qty_recibida)}.
     */
    @Transactional
    public void registrarIngresoPorCompra(Producto producto, Sucursal sucursal,
                                          BigDecimal cantidadRecibida, BigDecimal precioUnitario,
                                          Long responsableId) {
        InventarioSucursal inventario = getOrCreateInventario(producto, sucursal);

        BigDecimal qtyPrevia = inventario.getCantidadActual();
        BigDecimal costoPrevio = inventario.getCostoPromedioPonderado();
        BigDecimal qtyNueva = qtyPrevia.add(cantidadRecibida);
        BigDecimal nuevoCosto = qtyPrevia.multiply(costoPrevio)
                .add(cantidadRecibida.multiply(precioUnitario))
                .divide(qtyNueva, 2, RoundingMode.HALF_UP);

        inventario.setCantidadActual(qtyNueva);
        inventario.setCostoPromedioPonderado(nuevoCosto);
        inventarioRepository.save(inventario);

        movimientoRepository.save(MovimientoInventario.builder()
                .inventario(inventario)
                .tipo(TipoMovimiento.INGRESO)
                .motivo(MotivoMovimiento.COMPRA)
                .cantidad(cantidadRecibida)
                .fecha(Instant.now())
                .responsable(usuarioRepository.getReferenceById(responsableId))
                .build());
    }

    /**
     * Aplica la salida de stock de una línea de venta (RF-13, RF-14): valida que
     * exista existencia suficiente, decrementa {@code cantidad_actual} y registra
     * el {@code MovimientoInventario} (RETIRO / VENTA). Sin fila de inventario o
     * con stock insuficiente lanza {@link StockInsuficienteException} — al ir
     * dentro de la transacción de {@code VentaService.crear}, revierte todo (RN-01).
     */
    @Transactional
    public void registrarSalidaPorVenta(Producto producto, Sucursal sucursal,
                                        BigDecimal cantidad, Long responsableId) {
        InventarioSucursal inventario = inventarioRepository
                .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
                .orElseThrow(StockInsuficienteException::new);
        if (inventario.getCantidadActual().compareTo(cantidad) < 0) {
            throw new StockInsuficienteException();
        }
        inventario.setCantidadActual(inventario.getCantidadActual().subtract(cantidad));
        inventarioRepository.save(inventario);

        movimientoRepository.save(MovimientoInventario.builder()
                .inventario(inventario)
                .tipo(TipoMovimiento.RETIRO)
                .motivo(MotivoMovimiento.VENTA)
                .cantidad(cantidad)
                .fecha(Instant.now())
                .responsable(usuarioRepository.getReferenceById(responsableId))
                .build());
    }

    /**
     * Aplica la salida de stock en la sucursal origen al despachar una
     * transferencia (RF-18, RF-19). Sin fila de inventario o stock insuficiente
     * lanza {@link StockInsuficienteException}.
     */
    @Transactional
    public void registrarSalidaPorTransferencia(Producto producto, Sucursal sucursalOrigen,
                                                BigDecimal cantidad, Long responsableId) {
        InventarioSucursal inventario = inventarioRepository
                .findByProductoIdAndSucursalId(producto.getId(), sucursalOrigen.getId())
                .orElseThrow(StockInsuficienteException::new);
        if (inventario.getCantidadActual().compareTo(cantidad) < 0) {
            throw new StockInsuficienteException();
        }
        inventario.setCantidadActual(inventario.getCantidadActual().subtract(cantidad));
        inventarioRepository.save(inventario);

        movimientoRepository.save(MovimientoInventario.builder()
                .inventario(inventario)
                .tipo(TipoMovimiento.RETIRO)
                .motivo(MotivoMovimiento.TRANSFERENCIA_SALIDA)
                .cantidad(cantidad)
                .fecha(Instant.now())
                .responsable(usuarioRepository.getReferenceById(responsableId))
                .build());
    }

    /**
     * Aplica el ingreso de stock en la sucursal destino al confirmar la
     * recepción (total o parcial) de una transferencia (RF-20, RF-21). No
     * recalcula {@code costo_promedio_ponderado}: RF-12 lo limita explícitamente
     * a las recepciones de compra (Módulo 2); una transferencia mueve stock ya
     * valorado, no genera una nueva compra.
     */
    @Transactional
    public void registrarIngresoPorTransferencia(Producto producto, Sucursal sucursalDestino,
                                                 BigDecimal cantidad, Long responsableId) {
        InventarioSucursal inventario = getOrCreateInventario(producto, sucursalDestino);
        inventario.setCantidadActual(inventario.getCantidadActual().add(cantidad));
        inventarioRepository.save(inventario);

        movimientoRepository.save(MovimientoInventario.builder()
                .inventario(inventario)
                .tipo(TipoMovimiento.INGRESO)
                .motivo(MotivoMovimiento.TRANSFERENCIA_ENTRADA)
                .cantidad(cantidad)
                .fecha(Instant.now())
                .responsable(usuarioRepository.getReferenceById(responsableId))
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<InventarioResponse> listarInventarioSucursal(Long branchId, Pageable pageable) {
        sucursalService.getEntityById(branchId);
        return PageResponse.from(
                inventarioRepository.findBySucursalId(branchId, pageable).map(this::toResponse));
    }

    @Transactional
    public InventarioResponse configurarStockMinimo(Long productId, MinStockRequest request) {
        Long branchId = resolverSucursal(request.branchId());
        currentUser.assertPuedeOperarSobreSucursal(branchId);

        Producto producto = productoRepository.findById(productId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productId));
        Sucursal sucursal = sucursalService.getEntityById(branchId);

        InventarioSucursal inventario = getOrCreateInventario(producto, sucursal);
        inventario.setStockMinimo(request.stockMinimo());
        return toResponse(inventarioRepository.save(inventario));
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
            throw new AccessDeniedException("No puede configurar el stock mínimo de otra sucursal");
        }
        return propia;
    }

    private InventarioSucursal getOrCreateInventario(Producto producto, Sucursal sucursal) {
        return inventarioRepository
                .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
                .orElseGet(() -> inventarioRepository.save(InventarioSucursal.builder()
                        .producto(producto)
                        .sucursal(sucursal)
                        .cantidadActual(BigDecimal.ZERO)
                        .stockMinimo(BigDecimal.ZERO)
                        .costoPromedioPonderado(BigDecimal.ZERO)
                        .build()));
    }

    private boolean estaBajoMinimo(InventarioSucursal inventario) {
        return inventario.getCantidadActual().compareTo(inventario.getStockMinimo()) < 0;
    }

    private InventarioResponse toResponse(InventarioSucursal inventario) {
        return new InventarioResponse(
                inventario.getProducto().getId(),
                inventario.getProducto().getSku(),
                inventario.getProducto().getNombre(),
                inventario.getSucursal().getId(),
                inventario.getSucursal().getNombre(),
                inventario.getCantidadActual(),
                inventario.getStockMinimo(),
                inventario.getCostoPromedioPonderado(),
                estaBajoMinimo(inventario));
    }
}
