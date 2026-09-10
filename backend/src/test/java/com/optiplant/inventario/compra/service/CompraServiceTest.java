package com.optiplant.inventario.compra.service;

import com.optiplant.inventario.common.exception.ConflictoEstadoException;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.compra.dto.PurchaseOrderLineRequest;
import com.optiplant.inventario.compra.dto.PurchaseOrderRequest;
import com.optiplant.inventario.compra.dto.PurchaseOrderResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
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
class CompraServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private ProveedorService proveedorService;
    @Mock
    private ProductoService productoService;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private CompraService compraService;

    private final Proveedor proveedor = Proveedor.builder().id(1L).nombre("Ferretería").build();
    private final Sucursal sucursal = Sucursal.builder().id(1L).nombre("Norte").ciudad("Cali").build();
    private final Producto productoA = Producto.builder()
            .id(10L).sku("A").nombre("Prod A").unidadMedidaBase("u").build();
    private final Producto productoB = Producto.builder()
            .id(11L).sku("B").nombre("Prod B").unidadMedidaBase("u").build();

    private OrdenCompra ordenPendiente(EstadoOrdenCompra estado) {
        OrdenCompra orden = OrdenCompra.builder()
                .id(99L).proveedor(proveedor).sucursal(sucursal)
                .fecha(Instant.now()).estado(estado)
                .build();
        orden.addDetalle(OrdenCompraDetalle.builder()
                .producto(productoA).cantidad(new BigDecimal("5"))
                .precioUnitario(new BigDecimal("100")).descuento(BigDecimal.ZERO).build());
        orden.addDetalle(OrdenCompraDetalle.builder()
                .producto(productoB).cantidad(new BigDecimal("3"))
                .precioUnitario(new BigDecimal("50")).descuento(BigDecimal.ZERO).build());
        return orden;
    }

    @Test
    void crear_adminSinBranchId_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(true);
        PurchaseOrderRequest request = new PurchaseOrderRequest(
                1L, null, null,
                List.of(new PurchaseOrderLineRequest(10L, BigDecimal.TEN, BigDecimal.TEN, null)));

        assertThrows(ValidacionException.class, () -> compraService.crear(request));
    }

    @Test
    void crear_noAdminConSucursalAjena_lanzaAccessDenied() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        PurchaseOrderRequest request = new PurchaseOrderRequest(
                1L, 2L, null,
                List.of(new PurchaseOrderLineRequest(10L, BigDecimal.TEN, BigDecimal.TEN, null)));

        assertThrows(AccessDeniedException.class, () -> compraService.crear(request));
    }

    @Test
    void crear_ok_creaOrdenPendienteConDescuentoCeroPorDefecto() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(proveedorService.getEntityById(1L)).thenReturn(proveedor);
        when(sucursalService.getEntityById(1L)).thenReturn(sucursal);
        when(productoService.getEntityById(10L)).thenReturn(productoA);
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(i -> {
            OrdenCompra o = i.getArgument(0);
            o.setId(77L);
            return o;
        });

        PurchaseOrderRequest request = new PurchaseOrderRequest(
                1L, 1L, "30 días",
                List.of(new PurchaseOrderLineRequest(10L, new BigDecimal("5"), new BigDecimal("200"), null)));

        PurchaseOrderResponse response = compraService.crear(request);

        assertEquals(77L, response.id());
        assertEquals(EstadoOrdenCompra.PENDIENTE, response.estado());
        assertEquals(1, response.lineas().size());
        assertEquals(0, response.lineas().get(0).descuento().compareTo(BigDecimal.ZERO));
    }

    @Test
    void confirmarRecepcion_ordenNoPendiente_lanzaConflictoYNoTocaInventario() {
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.of(ordenPendiente(EstadoOrdenCompra.RECIBIDA)));
        when(currentUser.isAdmin()).thenReturn(true);

        assertThrows(ConflictoEstadoException.class, () -> compraService.confirmarRecepcion(99L));

        verify(inventarioService, never())
                .registrarIngresoPorCompra(any(), any(), any(), any(), any());
    }

    @Test
    void confirmarRecepcion_ok_aplicaIngresoPorLineaYPasaARecibida() {
        OrdenCompra orden = ordenPendiente(EstadoOrdenCompra.PENDIENTE);
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.of(orden));
        when(currentUser.isAdmin()).thenReturn(true);
        when(currentUser.usuarioId()).thenReturn(7L);
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderResponse response = compraService.confirmarRecepcion(99L);

        assertEquals(EstadoOrdenCompra.RECIBIDA, response.estado());
        verify(inventarioService, times(2))
                .registrarIngresoPorCompra(any(), eq(sucursal), any(), any(), eq(7L));
        verify(inventarioService).registrarIngresoPorCompra(
                eq(productoA), eq(sucursal), eq(new BigDecimal("5")), eq(new BigDecimal("100")), eq(7L));
        verify(inventarioService).registrarIngresoPorCompra(
                eq(productoB), eq(sucursal), eq(new BigDecimal("3")), eq(new BigDecimal("50")), eq(7L));
    }

    @Test
    void confirmarRecepcion_noAdminSucursalAjena_lanzaNoEncontrado() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(99L).proveedor(proveedor)
                .sucursal(Sucursal.builder().id(2L).nombre("Sur").build())
                .fecha(Instant.now()).estado(EstadoOrdenCompra.PENDIENTE)
                .build();
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.of(orden));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);

        assertThrows(RecursoNoEncontradoException.class, () -> compraService.confirmarRecepcion(99L));
    }
}
