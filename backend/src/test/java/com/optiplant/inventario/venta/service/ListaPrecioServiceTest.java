package com.optiplant.inventario.venta.service;

import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.service.ProductoService;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.venta.dto.PriceListItemRequest;
import com.optiplant.inventario.venta.dto.PriceListRequest;
import com.optiplant.inventario.venta.dto.PriceListResponse;
import com.optiplant.inventario.venta.dto.PriceListUpdateRequest;
import com.optiplant.inventario.venta.entity.ListaPrecio;
import com.optiplant.inventario.venta.entity.ListaPrecioDetalle;
import com.optiplant.inventario.venta.repository.ListaPrecioDetalleRepository;
import com.optiplant.inventario.venta.repository.ListaPrecioRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaPrecioServiceTest {

    @Mock
    private ListaPrecioRepository listaPrecioRepository;
    @Mock
    private ListaPrecioDetalleRepository listaPrecioDetalleRepository;
    @Mock
    private ProductoService productoService;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private ListaPrecioService listaPrecioService;

    private final Producto producto = Producto.builder()
            .id(10L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("u").build();

    @Test
    void crear_conProductoRepetido_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoService.getEntityById(10L)).thenReturn(producto);

        PriceListRequest request = new PriceListRequest("Lista", null, List.of(
                new PriceListItemRequest(10L, new BigDecimal("100")),
                new PriceListItemRequest(10L, new BigDecimal("120"))));

        assertThrows(ValidacionException.class, () -> listaPrecioService.crear(request));
    }

    @Test
    void crear_noAdminConSucursalAjena_lanzaAccessDenied() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);

        PriceListRequest request = new PriceListRequest("Lista", 2L, List.of(
                new PriceListItemRequest(10L, new BigDecimal("100"))));

        assertThrows(AccessDeniedException.class, () -> listaPrecioService.crear(request));
    }

    @Test
    void crear_ok_listaGlobal() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(listaPrecioRepository.save(any(ListaPrecio.class))).thenAnswer(i -> {
            ListaPrecio l = i.getArgument(0);
            l.setId(3L);
            return l;
        });

        PriceListResponse response = listaPrecioService.crear(new PriceListRequest(
                "Lista General", null, List.of(new PriceListItemRequest(10L, new BigDecimal("900")))));

        assertEquals(3L, response.id());
        assertNull(response.branchId());
        assertEquals(1, response.items().size());
    }

    private final ListaPrecio lista = ListaPrecio.builder().id(5L).nombre("Lista General").build();

    @Test
    void resolverPrecio_listaInexistente_lanzaNoEncontrado() {
        when(listaPrecioRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> listaPrecioService.resolverPrecio(5L, 10L));
    }

    @Test
    void resolverPrecio_productoSinPrecioEnLista_lanzaValidacion() {
        when(listaPrecioRepository.findById(5L)).thenReturn(Optional.of(lista));
        when(listaPrecioDetalleRepository.findByListaIdAndProductoId(5L, 10L)).thenReturn(Optional.empty());

        assertThrows(ValidacionException.class, () -> listaPrecioService.resolverPrecio(5L, 10L));
    }

    @Test
    void resolverPrecio_ok_devuelveElPrecioDeLaLista() {
        when(listaPrecioRepository.findById(5L)).thenReturn(Optional.of(lista));
        when(listaPrecioDetalleRepository.findByListaIdAndProductoId(5L, 10L))
                .thenReturn(Optional.of(ListaPrecioDetalle.builder().precio(new BigDecimal("900")).build()));

        assertEquals(0, listaPrecioService.resolverPrecio(5L, 10L).compareTo(new BigDecimal("900")));
    }

    @Test
    void actualizar_listaInexistente_lanzaNoEncontrado() {
        when(listaPrecioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> listaPrecioService.actualizar(99L,
                new PriceListUpdateRequest("Nueva", List.of(new PriceListItemRequest(10L, new BigDecimal("100"))))));
    }

    @Test
    void actualizar_noAdminListaGlobal_lanzaAccessDenied() {
        when(listaPrecioRepository.findById(5L)).thenReturn(Optional.of(lista));
        when(currentUser.isAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> listaPrecioService.actualizar(5L,
                new PriceListUpdateRequest("Nueva", List.of(new PriceListItemRequest(10L, new BigDecimal("100"))))));
    }

    @Test
    void actualizar_noAdminSucursalAjena_lanzaAccessDenied() {
        ListaPrecio listaDeOtraSucursal = ListaPrecio.builder().id(6L).nombre("Sur")
                .sucursal(Sucursal.builder().id(2L).nombre("Sur").build()).build();
        when(listaPrecioRepository.findById(6L)).thenReturn(Optional.of(listaDeOtraSucursal));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> listaPrecioService.actualizar(6L,
                new PriceListUpdateRequest("Nueva", List.of(new PriceListItemRequest(10L, new BigDecimal("100"))))));
    }

    @Test
    void actualizar_conProductoRepetido_lanzaValidacion() {
        when(listaPrecioRepository.findById(5L)).thenReturn(Optional.of(lista));
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoService.getEntityById(10L)).thenReturn(producto);

        PriceListUpdateRequest request = new PriceListUpdateRequest("Nueva", List.of(
                new PriceListItemRequest(10L, new BigDecimal("100")),
                new PriceListItemRequest(10L, new BigDecimal("120"))));

        assertThrows(ValidacionException.class, () -> listaPrecioService.actualizar(5L, request));
    }

    @Test
    void actualizar_ok_reemplazaNombreYItems() {
        ListaPrecio listaConItem = ListaPrecio.builder().id(5L).nombre("Lista General").build();
        listaConItem.addDetalle(ListaPrecioDetalle.builder().id(1L).producto(producto).precio(new BigDecimal("900")).build());
        when(listaPrecioRepository.findById(5L)).thenReturn(Optional.of(listaConItem));
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(listaPrecioRepository.save(any(ListaPrecio.class))).thenAnswer(i -> i.getArgument(0));

        PriceListResponse response = listaPrecioService.actualizar(5L,
                new PriceListUpdateRequest("Lista Actualizada", List.of(new PriceListItemRequest(10L, new BigDecimal("950")))));

        assertEquals("Lista Actualizada", response.nombre());
        assertEquals(1, response.items().size());
        assertEquals(0, response.items().get(0).precio().compareTo(new BigDecimal("950")));
    }
}
