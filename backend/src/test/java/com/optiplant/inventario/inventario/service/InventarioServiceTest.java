package com.optiplant.inventario.inventario.service;

import com.optiplant.inventario.common.exception.StockInsuficienteException;
import com.optiplant.inventario.common.exception.ValidacionException;
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
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioSucursalRepository inventarioRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private InventarioService inventarioService;

    private final Producto producto = Producto.builder()
            .id(10L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("unidad").build();
    private final Sucursal sucursal = Sucursal.builder()
            .id(1L).nombre("Norte").ciudad("Cali").build();

    private InventarioSucursal inventarioCon(BigDecimal cantidad, BigDecimal minimo) {
        return InventarioSucursal.builder()
                .id(100L).producto(producto).sucursal(sucursal)
                .cantidadActual(cantidad).stockMinimo(minimo)
                .costoPromedioPonderado(BigDecimal.ZERO)
                .build();
    }

    private MovimientoRequest request(TipoMovimiento tipo, MotivoMovimiento motivo, String cantidad) {
        return new MovimientoRequest(10L, 1L, tipo, motivo, new BigDecimal(cantidad));
    }

    @Test
    void ingreso_sumaCantidadActualYRegistraMovimiento() {
        InventarioSucursal inventario = inventarioCon(new BigDecimal("5"), new BigDecimal("2"));
        when(currentUser.usuarioId()).thenReturn(7L);
        when(productoRepository.findById(10L)).thenReturn(java.util.Optional.of(producto));
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(Usuario.builder().id(7L).build());
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> {
            MovimientoInventario m = i.getArgument(0);
            m.setId(500L);
            return m;
        });

        MovimientoResponse response =
                inventarioService.registrarMovimiento(request(TipoMovimiento.INGRESO, MotivoMovimiento.AJUSTE, "10"));

        assertEquals(0, inventario.getCantidadActual().compareTo(new BigDecimal("15")));
        assertEquals(0, response.cantidadActual().compareTo(new BigDecimal("15")));
        assertFalse(response.alertaStockBajo());

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(TipoMovimiento.INGRESO, captor.getValue().getTipo());
        assertEquals(MotivoMovimiento.AJUSTE, captor.getValue().getMotivo());
        assertEquals(0, captor.getValue().getCantidad().compareTo(new BigDecimal("10")));
    }

    @Test
    void retiroSinStockSuficiente_lanzaStockInsuficienteYNoPersiste() {
        InventarioSucursal inventario = inventarioCon(new BigDecimal("3"), new BigDecimal("1"));
        when(productoRepository.findById(10L)).thenReturn(java.util.Optional.of(producto));
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.registrarMovimiento(
                        request(TipoMovimiento.RETIRO, MotivoMovimiento.VENTA, "10")));

        assertEquals(0, inventario.getCantidadActual().compareTo(new BigDecimal("3")));
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void retiroQueDejaBajoElMinimo_marcaAlertaStockBajo() {
        InventarioSucursal inventario = inventarioCon(new BigDecimal("10"), new BigDecimal("8"));
        when(currentUser.usuarioId()).thenReturn(7L);
        when(productoRepository.findById(10L)).thenReturn(java.util.Optional.of(producto));
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(Usuario.builder().id(7L).build());
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoResponse response =
                inventarioService.registrarMovimiento(request(TipoMovimiento.RETIRO, MotivoMovimiento.MERMA, "5"));

        assertEquals(0, response.cantidadActual().compareTo(new BigDecimal("5")));
        assertTrue(response.alertaStockBajo());
    }

    @Test
    void motivoDeTransferencia_esRechazado() {
        assertThrows(ValidacionException.class,
                () -> inventarioService.registrarMovimiento(
                        request(TipoMovimiento.INGRESO, MotivoMovimiento.TRANSFERENCIA_ENTRADA, "5")));
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void primerMovimientoDeUnParProductoSucursal_creaInventarioEnCero() {
        when(currentUser.usuarioId()).thenReturn(7L);
        when(productoRepository.findById(10L)).thenReturn(java.util.Optional.of(producto));
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L)).thenReturn(java.util.Optional.empty());
        when(usuarioRepository.getReferenceById(7L)).thenReturn(Usuario.builder().id(7L).build());
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoResponse response =
                inventarioService.registrarMovimiento(request(TipoMovimiento.INGRESO, MotivoMovimiento.COMPRA, "20"));

        assertEquals(0, response.cantidadActual().compareTo(new BigDecimal("20")));
    }

    @Test
    void configurarStockMinimo_adminSinBranchId_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(true);

        assertThrows(ValidacionException.class,
                () -> inventarioService.configurarStockMinimo(10L, new MinStockRequest(BigDecimal.TEN, null)));
    }

    @Test
    void configurarStockMinimo_noAdminConSucursalAjena_lanzaAccessDenied() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);

        assertThrows(AccessDeniedException.class,
                () -> inventarioService.configurarStockMinimo(10L, new MinStockRequest(BigDecimal.TEN, 2L)));
    }

    @Test
    void configurarStockMinimo_actualizaElValor() {
        InventarioSucursal inventario = inventarioCon(new BigDecimal("4"), new BigDecimal("5"));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        when(productoRepository.findById(10L)).thenReturn(java.util.Optional.of(producto));
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));

        inventarioService.configurarStockMinimo(10L, new MinStockRequest(new BigDecimal("15.00"), null));

        assertEquals(0, inventario.getStockMinimo().compareTo(new BigDecimal("15.00")));
    }

    // --- registrarIngresoPorCompra (RF-10 / RF-12) --------------------------------

    @Test
    void ingresoPorCompra_primerIngreso_costoPromedioIgualAlPrecio() {
        InventarioSucursal inventario = inventarioCon(BigDecimal.ZERO, BigDecimal.ZERO);
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(Usuario.builder().id(7L).build());
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        inventarioService.registrarIngresoPorCompra(
                producto, sucursal, new BigDecimal("100"), new BigDecimal("500"), 7L);

        assertEquals(0, inventario.getCantidadActual().compareTo(new BigDecimal("100")));
        assertEquals(0, inventario.getCostoPromedioPonderado().compareTo(new BigDecimal("500.00")));

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(TipoMovimiento.INGRESO, captor.getValue().getTipo());
        assertEquals(MotivoMovimiento.COMPRA, captor.getValue().getMotivo());
    }

    @Test
    void ingresoPorCompra_segundoIngreso_promedioPonderado() {
        InventarioSucursal inventario = inventarioCon(new BigDecimal("100"), BigDecimal.ZERO);
        inventario.setCostoPromedioPonderado(new BigDecimal("500"));
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(Usuario.builder().id(7L).build());
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        // (100*500 + 100*600) / 200 = 550.00
        inventarioService.registrarIngresoPorCompra(
                producto, sucursal, new BigDecimal("100"), new BigDecimal("600"), 7L);

        assertEquals(0, inventario.getCantidadActual().compareTo(new BigDecimal("200")));
        assertEquals(0, inventario.getCostoPromedioPonderado().compareTo(new BigDecimal("550.00")));
    }

    @Test
    void ingresoPorCompra_redondeaCostoAHalfUp() {
        InventarioSucursal inventario = inventarioCon(new BigDecimal("1"), BigDecimal.ZERO);
        inventario.setCostoPromedioPonderado(new BigDecimal("100"));
        when(inventarioRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(java.util.Optional.of(inventario));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(Usuario.builder().id(7L).build());
        when(inventarioRepository.save(any(InventarioSucursal.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        // (1*100 + 2*110) / 3 = 320/3 = 106.666... -> 106.67
        inventarioService.registrarIngresoPorCompra(
                producto, sucursal, new BigDecimal("2"), new BigDecimal("110"), 7L);

        assertEquals(0, inventario.getCostoPromedioPonderado().compareTo(new BigDecimal("106.67")));
    }
}
