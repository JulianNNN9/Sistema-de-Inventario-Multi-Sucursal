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
