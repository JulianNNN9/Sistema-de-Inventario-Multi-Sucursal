package com.optiplant.inventario.producto.service;

import com.optiplant.inventario.common.exception.ConflictoEstadoException;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.producto.dto.ProductoRequest;
import com.optiplant.inventario.producto.dto.ProductoResponse;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.repository.ProductoRepository;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.repository.SucursalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private InventarioSucursalRepository inventarioSucursalRepository;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private ProductoService productoService;

    private final Producto producto = Producto.builder()
            .id(5L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("unidad").build();

    @Test
    void crear_conSkuExistente_lanzaValidacion() {
        when(productoRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThrows(ValidacionException.class,
                () -> productoService.crear(new ProductoRequest("SKU-1", "Tornillo", "unidad")));
    }

    @Test
    void crear_admin_persisteYDevuelveResponseSinInventario() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoRepository.existsBySku("SKU-2")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            p.setId(9L);
            return p;
        });

        ProductoResponse response =
                productoService.crear(new ProductoRequest("SKU-2", "Tuerca", "unidad"));

        assertEquals(9L, response.id());
        assertEquals("SKU-2", response.sku());
        verify(inventarioSucursalRepository, never()).save(any());
    }

    @Test
    void crear_noAdmin_creaFilaDeInventarioEnSuPropiaSucursal() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(3L);
        when(productoRepository.existsBySku("SKU-2")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            p.setId(9L);
            return p;
        });
        Sucursal sucursal = Sucursal.builder().id(3L).build();
        when(sucursalRepository.getReferenceById(3L)).thenReturn(sucursal);

        productoService.crear(new ProductoRequest("SKU-2", "Tuerca", "unidad"));

        verify(inventarioSucursalRepository).save(any(InventarioSucursal.class));
    }

    @Test
    void eliminar_conInventarioAsociado_lanzaConflicto() {
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
        when(inventarioSucursalRepository.existsByProductoId(5L)).thenReturn(true);

        assertThrows(ConflictoEstadoException.class, () -> productoService.eliminar(5L));
        verify(productoRepository, never()).delete(any());
    }

    @Test
    void eliminar_sinInventario_borraProducto() {
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
        when(inventarioSucursalRepository.existsByProductoId(5L)).thenReturn(false);

        productoService.eliminar(5L);

        verify(productoRepository).delete(producto);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> productoService.eliminar(99L));
    }

    @Test
    void listar_noAdmin_ignoraBranchIdParamYUsaSuSucursal() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        when(productoRepository.findAllInSucursal(eq(1L), any(), any(Pageable.class))).thenReturn(Page.empty());

        productoService.listar(99L, null, PageRequest.of(0, 20));

        verify(productoRepository).findAllInSucursal(eq(1L), any(), any(Pageable.class));
        verify(productoRepository, never()).buscar(any(), any(Pageable.class));
    }

    @Test
    void listar_admin_conBranchId_filtraPorEsaSucursal() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoRepository.findAllInSucursal(eq(7L), any(), any(Pageable.class))).thenReturn(Page.empty());

        productoService.listar(7L, null, PageRequest.of(0, 20));

        verify(productoRepository).findAllInSucursal(eq(7L), any(), any(Pageable.class));
    }

    @Test
    void listar_admin_sinBranchId_devuelveTodoElCatalogo() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoRepository.buscar(any(), any(Pageable.class))).thenReturn(Page.empty());

        productoService.listar(null, null, PageRequest.of(0, 20));

        verify(productoRepository).buscar(any(), any(Pageable.class));
        verify(productoRepository, never()).findAllInSucursal(any(), any(), any());
    }
}
