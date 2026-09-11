package com.optiplant.inventario.venta.service;

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
import com.optiplant.inventario.venta.dto.SaleRequest;
import com.optiplant.inventario.venta.dto.SaleResponse;
import com.optiplant.inventario.venta.entity.Venta;
import com.optiplant.inventario.venta.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ListaPrecioService listaPrecioService;
    @Mock
    private ProductoService productoService;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private InventarioSucursalRepository inventarioSucursalRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private VentaService ventaService;

    private final Sucursal sucursal = Sucursal.builder().id(1L).nombre("Norte").ciudad("Cali").build();
    private final Producto producto = Producto.builder()
            .id(10L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("u").build();
    private final Usuario usuario = Usuario.builder().id(7L).nombre("Ana").build();

    private InventarioSucursal inventarioCon(String cantidad) {
        return InventarioSucursal.builder()
                .producto(producto).sucursal(sucursal)
                .cantidadActual(new BigDecimal(cantidad))
                .stockMinimo(BigDecimal.ZERO).costoPromedioPonderado(BigDecimal.ZERO)
                .build();
    }

    private SaleLineRequest linea(String cantidad, Long priceListId, String precioUnitario, String descuento) {
        return new SaleLineRequest(10L, new BigDecimal(cantidad), priceListId,
                precioUnitario != null ? new BigDecimal(precioUnitario) : null,
                descuento != null ? new BigDecimal(descuento) : null);
    }

    @Test
    void crear_adminSinBranchId_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(true);
        SaleRequest request = new SaleRequest(null, List.of(linea("1", null, "100", null)));

        assertThrows(ValidacionException.class, () -> ventaService.crear(request));
    }

    @Test
    void crear_noAdminConSucursalAjena_lanzaAccessDenied() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        SaleRequest request = new SaleRequest(2L, List.of(linea("1", null, "100", null)));

        assertThrows(AccessDeniedException.class, () -> ventaService.crear(request));
    }

    @Test
    void crear_lineaSinPrecioNiLista_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(productoService.getEntityById(10L)).thenReturn(producto);

        SaleRequest request = new SaleRequest(1L, List.of(linea("5", null, null, null)));

        assertThrows(ValidacionException.class, () -> ventaService.crear(request));
    }

    @Test
    void crear_stockInsuficiente_lanzaYNoPersiste() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(Optional.of(inventarioCon("30")));

        SaleRequest request = new SaleRequest(1L, List.of(linea("50", null, "100", null)));

        assertThrows(StockInsuficienteException.class, () -> ventaService.crear(request));
        verify(ventaRepository, never()).save(any());
        verify(inventarioService, never()).registrarSalidaPorVenta(any(), any(), any(), any());
    }

    @Test
    void crear_mismoProductoEnDosLineas_validaStockAgregado() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(Optional.of(inventarioCon("10")));

        // 6 + 6 = 12 > 10, aunque cada línea individual (6) cabría
        SaleRequest request = new SaleRequest(1L, List.of(
                linea("6", null, "100", null), linea("6", null, "100", null)));

        assertThrows(StockInsuficienteException.class, () -> ventaService.crear(request));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crear_ok_resuelvePrecioDeListaCalculaTotalNetoYAplicaSalida() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(currentUser.usuarioId()).thenReturn(7L);
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(listaPrecioService.resolverPrecio(5L, 10L)).thenReturn(new BigDecimal("900"));
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 1L))
                .thenReturn(Optional.of(inventarioCon("100")));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setId(99L);
            return v;
        });

        // 10 unidades * 900 * (1 - 5/100) = 8550.00
        SaleRequest request = new SaleRequest(1L, List.of(linea("10", 5L, null, "5")));
        SaleResponse response = ventaService.crear(request);

        assertEquals(99L, response.id());
        assertEquals(0, response.total().compareTo(new BigDecimal("8550.00")));
        assertEquals(1, response.lineas().size());
        assertEquals(0, response.lineas().get(0).subtotal().compareTo(new BigDecimal("8550.00")));

        verify(inventarioService, times(1)).registrarSalidaPorVenta(
                eq(producto), eq(sucursal), eq(new BigDecimal("10")), eq(7L));
    }
}
