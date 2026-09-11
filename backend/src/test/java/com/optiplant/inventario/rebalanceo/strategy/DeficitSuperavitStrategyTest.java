package com.optiplant.inventario.rebalanceo.strategy;

import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.rebalanceo.dto.Sugerencia;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.transferencia.entity.Urgencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RF-31: el emparejamiento en sí (no la prefiltración SQL, ya cubierta por el
 * repositorio) — magnitud del déficit, excedente disponible que se consume
 * entre asignaciones, y la regla de urgencia.
 */
@ExtendWith(MockitoExtension.class)
class DeficitSuperavitStrategyTest {

    @Mock
    private InventarioSucursalRepository inventarioSucursalRepository;

    @InjectMocks
    private DeficitSuperavitStrategy strategy;

    private final Producto productoA = Producto.builder().id(1L).sku("SKU-A").nombre("Producto A").unidadMedidaBase("u").build();
    private final Producto productoB = Producto.builder().id(2L).sku("SKU-B").nombre("Producto B").unidadMedidaBase("u").build();
    private final Sucursal norte = Sucursal.builder().id(1L).nombre("Norte").build();
    private final Sucursal sur = Sucursal.builder().id(2L).nombre("Sur").build();
    private final Sucursal este = Sucursal.builder().id(3L).nombre("Este").build();

    private InventarioSucursal inv(Producto producto, Sucursal sucursal, String cantidadActual, String stockMinimo) {
        return InventarioSucursal.builder()
                .producto(producto).sucursal(sucursal)
                .cantidadActual(new BigDecimal(cantidadActual))
                .stockMinimo(new BigDecimal(stockMinimo))
                .build();
    }

    private void mockCandidatos(InventarioSucursal... filas) {
        when(inventarioSucursalRepository.findCandidatosRebalanceo(any())).thenReturn(List.of(filas));
    }

    @Test
    void calcular_sinCandidatos_devuelveListaVacia() {
        mockCandidatos();

        assertTrue(strategy.calcular().isEmpty());
    }

    @Test
    void calcular_soloDeficitSinSuperavit_noGeneraSugerencia() {
        // Norte en déficit (mínimo 10, tiene 2); nadie con superávit para el producto.
        mockCandidatos(inv(productoA, norte, "2", "10"));

        assertTrue(strategy.calcular().isEmpty());
    }

    @Test
    void calcular_soloSuperavitSinDeficit_noGeneraSugerencia() {
        // Sur con superávit (mínimo 10, tiene 20 > 10*1.5); nadie en déficit.
        mockCandidatos(inv(productoA, sur, "20", "10"));

        assertTrue(strategy.calcular().isEmpty());
    }

    @Test
    void calcular_emparejaDeficitConElSuperavitDeMayorExcedenteDisponible() {
        // Norte: déficit de 8 (mínimo 10, tiene 2).
        // Sur: excedente 10 (mínimo 10, tiene 20). Este: excedente 20 (mínimo 10, tiene 30).
        mockCandidatos(
                inv(productoA, norte, "2", "10"),
                inv(productoA, sur, "20", "10"),
                inv(productoA, este, "30", "10"));

        List<Sugerencia> sugerencias = strategy.calcular();

        assertEquals(1, sugerencias.size());
        Sugerencia s = sugerencias.get(0);
        assertEquals(1L, s.productId());
        assertEquals(3L, s.sucursalOrigenId(), "debe elegir Este: tiene el mayor excedente (20 > 10)");
        assertEquals(1L, s.sucursalDestinoId());
        assertEquals(0, s.cantidadSugerida().compareTo(new BigDecimal("8")), "min(déficit=8, excedente=20) = 8");
    }

    @Test
    void calcular_limitaLaCantidadAlExcedenteCuandoElDeficitEsMayor() {
        // Norte: déficit de 18 (mínimo 20, tiene 2). Sur: excedente de solo 5 (mínimo 8, tiene 13 > 8*1.5=12).
        mockCandidatos(
                inv(productoA, norte, "2", "20"),
                inv(productoA, sur, "13", "8"));

        List<Sugerencia> sugerencias = strategy.calcular();

        assertEquals(1, sugerencias.size());
        assertEquals(0, sugerencias.get(0).cantidadSugerida().compareTo(new BigDecimal("5")),
                "min(déficit=18, excedente=5) = 5");
    }

    @Test
    void calcular_consumeElExcedenteEntreVariosDeficitsDelMismoProducto() {
        // Sur: excedente único de 12 (mínimo 10, tiene 22).
        // Norte: déficit de 8 (mayor, se atiende primero). Este: déficit de 5.
        mockCandidatos(
                inv(productoA, norte, "2", "10"),
                inv(productoA, este, "5", "10"),
                inv(productoA, sur, "22", "10"));

        List<Sugerencia> sugerencias = strategy.calcular();

        assertEquals(2, sugerencias.size());
        Sugerencia paraNorte = sugerencias.stream().filter(s -> s.sucursalDestinoId().equals(1L)).findFirst().orElseThrow();
        Sugerencia paraEste = sugerencias.stream().filter(s -> s.sucursalDestinoId().equals(3L)).findFirst().orElseThrow();

        assertEquals(0, paraNorte.cantidadSugerida().compareTo(new BigDecimal("8")), "Norte (déficit mayor) se sirve primero, completo");
        assertEquals(0, paraEste.cantidadSugerida().compareTo(new BigDecimal("4")), "a Este solo le queda el remanente: 12 - 8 = 4");
    }

    @Test
    void calcular_sinExcedenteRestante_elSegundoDeficitNoGeneraSugerencia() {
        // Sur: excedente único de 8. Norte (déficit 8, mayor) se lo queda todo; Este (déficit 5) se queda sin nada.
        mockCandidatos(
                inv(productoA, norte, "2", "10"),
                inv(productoA, este, "5", "10"),
                inv(productoA, sur, "18", "10"));

        List<Sugerencia> sugerencias = strategy.calcular();

        assertEquals(1, sugerencias.size());
        assertEquals(1L, sugerencias.get(0).sucursalDestinoId());
    }

    @Test
    void calcular_urgenciaAltaCuandoElDestinoEstaEnCero() {
        mockCandidatos(
                inv(productoA, norte, "0", "10"),
                inv(productoA, sur, "20", "10"));

        assertEquals(Urgencia.ALTA, strategy.calcular().get(0).urgencia());
    }

    @Test
    void calcular_urgenciaMediaCuandoElDestinoTieneAlgoDeStock() {
        mockCandidatos(
                inv(productoA, norte, "3", "10"),
                inv(productoA, sur, "20", "10"));

        assertEquals(Urgencia.MEDIA, strategy.calcular().get(0).urgencia());
    }

    @Test
    void calcular_ordenaLasSugerenciasConUrgenciaAltaPrimero() {
        mockCandidatos(
                // Producto A: Norte en 3 (MEDIA).
                inv(productoA, norte, "3", "10"), inv(productoA, sur, "20", "10"),
                // Producto B: Este en 0 (ALTA).
                inv(productoB, este, "0", "10"), inv(productoB, sur, "20", "10"));

        List<Sugerencia> sugerencias = strategy.calcular();

        assertEquals(2, sugerencias.size());
        assertEquals(Urgencia.ALTA, sugerencias.get(0).urgencia());
        assertEquals(Urgencia.MEDIA, sugerencias.get(1).urgencia());
    }

    @Test
    void calcular_evaluaCadaProductoDeFormaIndependiente() {
        // Superávit de Producto B en Sur no debe usarse para cubrir el déficit de Producto A en Norte.
        mockCandidatos(
                inv(productoA, norte, "2", "10"),
                inv(productoB, sur, "20", "10"));

        assertTrue(strategy.calcular().isEmpty());
    }
}
