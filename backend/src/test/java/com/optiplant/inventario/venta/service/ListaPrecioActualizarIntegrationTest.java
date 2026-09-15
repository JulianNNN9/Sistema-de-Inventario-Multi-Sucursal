package com.optiplant.inventario.venta.service;

import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.repository.ProductoRepository;
import com.optiplant.inventario.producto.service.ProductoService;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.venta.dto.PriceListItemRequest;
import com.optiplant.inventario.venta.dto.PriceListUpdateRequest;
import com.optiplant.inventario.venta.dto.PriceListResponse;
import com.optiplant.inventario.venta.entity.ListaPrecio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * {@link ListaPrecioService#actualizar} borra y reinserta los ítems en cada
 * edición (mismo patrón que motivó agregar {@code @Version}); usa un
 * {@code @DataJpaTest} real en vez de mocks porque el bug que cubre — Hibernate
 * ordena los INSERT de los nuevos ítems antes que estos DELETE dentro del mismo
 * flush, chocando contra {@code uq_lpd_lista_producto} cuando un
 * {@code productId} se repite entre la lista vieja y la nueva — solo se
 * manifiesta contra una base de datos real, nunca con repositorios mockeados.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(ListaPrecioService.class)
class ListaPrecioActualizarIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ListaPrecioService listaPrecioService;

    @MockBean
    private ProductoService productoService;
    @MockBean
    private SucursalService sucursalService;
    @MockBean
    private CurrentUser currentUser;

    @Test
    void actualizar_conProductoQueYaEstabaEnLaLista_noLanzaViolacionDeUnicidad() {
        Producto producto = productoRepository.save(
                Producto.builder().sku("SKU-1").nombre("Martillo").unidadMedidaBase("UNIDAD").build());
        Long listaId = entityManager.persistFlushFind(
                ListaPrecio.builder().nombre("Lista general").build()).getId();

        // Item inicial: producto ya en la lista con precio 100.
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoService.getEntityById(producto.getId())).thenReturn(producto);
        listaPrecioService.actualizar(listaId, new PriceListUpdateRequest(
                "Lista general", List.of(new PriceListItemRequest(producto.getId(), new BigDecimal("100")))));

        // entityManager.clear() simula que la segunda edición llega en una petición
        // HTTP separada (persistence context nuevo), no en la misma transacción que
        // la primera: así el segundo actualizar() vuelve a cargar la lista desde BD
        // en vez de reutilizar la instancia en memoria de la primera llamada.
        entityManager.flush();
        entityManager.getEntityManager().clear();

        // La edición vuelve a enviar el MISMO productId (caso normal: solo cambia el precio).
        PriceListResponse response = listaPrecioService.actualizar(listaId, new PriceListUpdateRequest(
                "Lista general", List.of(new PriceListItemRequest(producto.getId(), new BigDecimal("150")))));

        assertEquals(1, response.items().size());
        assertEquals(0, response.items().get(0).precio().compareTo(new BigDecimal("150")));
    }
}
