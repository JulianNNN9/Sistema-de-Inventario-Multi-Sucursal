package com.optiplant.inventario.dashboard.service;

import com.optiplant.inventario.dashboard.dto.BranchComparisonRow;
import com.optiplant.inventario.dashboard.dto.InventoryRotationItem;
import com.optiplant.inventario.dashboard.dto.RestockAlert;
import com.optiplant.inventario.dashboard.dto.SalesComparisonPoint;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.inventario.repository.MovimientoInventarioRepository;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.repository.SucursalRepository;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import com.optiplant.inventario.venta.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La agregación SQL (SUM/COUNT/subconsultas correlacionadas) ya vive y se
 * prueba a nivel de integración con Docker/Postgres (mismo criterio que el
 * Módulo 5). Estas pruebas cubren lo que sí es responsabilidad de
 * {@link DashboardService} en Java: el alcance por sucursal (RN de la Sección
 * 4.2: no-admin nunca ve otra sucursal), las ventanas de tiempo (RF-26/27/30)
 * y que los parámetros fijos (estados no terminales, tipo RETIRO) se pasen
 * exactamente como exige cada RF.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private InventarioSucursalRepository inventarioSucursalRepository;
    @Mock
    private TransferenciaRepository transferenciaRepository;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private DashboardService dashboardService;

    // --- sales-comparison (RF-26) --------------------------------------------------

    @Test
    void salesComparison_noAdmin_ignoraElParametroYUsaLaSucursalDelUsuario() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        when(ventaRepository.sumTotalEntreFechas(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);

        dashboardService.salesComparison(99L);

        verify(ventaRepository, org.mockito.Mockito.times(4))
                .sumTotalEntreFechas(eq(1L), any(), any());
    }

    @Test
    void salesComparison_admin_respetaElBranchIdDelParametro() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(ventaRepository.sumTotalEntreFechas(eq(2L), any(), any())).thenReturn(BigDecimal.ZERO);

        dashboardService.salesComparison(2L);

        verify(ventaRepository, org.mockito.Mockito.times(4))
                .sumTotalEntreFechas(eq(2L), any(), any());
    }

    @Test
    void salesComparison_generaCuatroMesesDelMasAntiguoAlActual() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(ventaRepository.sumTotalEntreFechas(isNull(), any(), any())).thenReturn(new BigDecimal("100"));

        List<SalesComparisonPoint> puntos = dashboardService.salesComparison(null);

        assertEquals(4, puntos.size());
        YearMonth actual = YearMonth.now(ZoneOffset.UTC);
        assertEquals(actual.minusMonths(3).toString(), puntos.get(0).periodo());
        assertEquals(actual.toString(), puntos.get(3).periodo());
        assertEquals(0, puntos.get(3).totalVentas().compareTo(new BigDecimal("100")));
    }

    @Test
    void salesComparison_cadaMesUsaUnRangoDeUnMesExacto() {
        when(currentUser.isAdmin()).thenReturn(true);
        ArgumentCaptor<Instant> desdeCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> hastaCaptor = ArgumentCaptor.forClass(Instant.class);
        when(ventaRepository.sumTotalEntreFechas(isNull(), desdeCaptor.capture(), hastaCaptor.capture()))
                .thenReturn(BigDecimal.ZERO);

        dashboardService.salesComparison(null);

        for (int i = 0; i < 4; i++) {
            Instant desde = desdeCaptor.getAllValues().get(i);
            Instant hasta = hastaCaptor.getAllValues().get(i);
            YearMonth mesEsperado = YearMonth.now(ZoneOffset.UTC).minusMonths(3 - i);
            assertEquals(mesEsperado.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(), desde);
            assertEquals(mesEsperado.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(), hasta);
        }
    }

    // --- inventory-rotation (RF-27) ------------------------------------------------

    @Test
    void inventoryRotation_pideTop5DeCadaExtremoFiltrandoPorRetiro() {
        when(currentUser.isAdmin()).thenReturn(true);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(movimientoInventarioRepository.topMayorRotacion(eq(3L), eq(TipoMovimiento.RETIRO), any(), pageableCaptor.capture()))
                .thenReturn(List.of());
        when(movimientoInventarioRepository.topMenorRotacion(eq(3L), eq(TipoMovimiento.RETIRO), any(), any()))
                .thenReturn(List.of());

        dashboardService.inventoryRotation(3L);

        assertEquals(5, pageableCaptor.getValue().getPageSize());
        verify(movimientoInventarioRepository).topMayorRotacion(eq(3L), eq(TipoMovimiento.RETIRO), any(), any());
        verify(movimientoInventarioRepository).topMenorRotacion(eq(3L), eq(TipoMovimiento.RETIRO), any(), any());
    }

    @Test
    void inventoryRotation_devuelveLasListasTalComoLasDaElRepositorio() {
        when(currentUser.isAdmin()).thenReturn(true);
        InventoryRotationItem masVendido = new InventoryRotationItem(10L, "SKU-1", "Tornillo", new BigDecimal("500"));
        InventoryRotationItem menosVendido = new InventoryRotationItem(20L, "SKU-2", "Tuerca", new BigDecimal("1"));
        when(movimientoInventarioRepository.topMayorRotacion(any(), any(), any(), any())).thenReturn(List.of(masVendido));
        when(movimientoInventarioRepository.topMenorRotacion(any(), any(), any(), any())).thenReturn(List.of(menosVendido));

        var response = dashboardService.inventoryRotation(null);

        assertEquals(List.of(masVendido), response.mayorRotacion());
        assertEquals(List.of(menosVendido), response.menorRotacion());
    }

    // --- active-transfers (RF-28) --------------------------------------------------

    @Test
    void activeTransfers_filtraExactamenteLosTresEstadosNoTerminales() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        ArgumentCaptor<List<EstadoTransferencia>> estadosCaptor = ArgumentCaptor.captor();
        when(transferenciaRepository.countActivasPorEstado(estadosCaptor.capture(), eq(1L))).thenReturn(List.of());

        dashboardService.activeTransfers(null);

        assertEquals(3, estadosCaptor.getValue().size());
        assertTrue(estadosCaptor.getValue().containsAll(List.of(
                EstadoTransferencia.PENDIENTE, EstadoTransferencia.EN_TRANSITO, EstadoTransferencia.CON_FALTANTES)));
        assertTrue(estadosCaptor.getValue().stream().noneMatch(
                e -> e == EstadoTransferencia.COMPLETADA || e == EstadoTransferencia.RECHAZADA
                        || e == EstadoTransferencia.CERRADA_AJUSTE || e == EstadoTransferencia.CERRADA_RECLAMACION
                        || e == EstadoTransferencia.REENVIO_SOLICITADO));
    }

    // --- restock-alerts (RF-29) -----------------------------------------------------

    @Test
    void restockAlerts_admin_sinBranchId_consultaTodasLasSucursales() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(inventarioSucursalRepository.restockAlerts(isNull())).thenReturn(List.of());

        dashboardService.restockAlerts(null);

        verify(inventarioSucursalRepository).restockAlerts(isNull());
    }

    @Test
    void restockAlerts_mapeaCamposDeInventarioSucursalARestockAlert() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        Producto producto = Producto.builder().id(10L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("u").build();
        Sucursal sucursal = Sucursal.builder().id(1L).nombre("Norte").build();
        InventarioSucursal inv = InventarioSucursal.builder()
                .id(5L).producto(producto).sucursal(sucursal)
                .cantidadActual(new BigDecimal("2")).stockMinimo(new BigDecimal("10"))
                .build();
        when(inventarioSucursalRepository.restockAlerts(1L)).thenReturn(List.of(inv));

        List<RestockAlert> alerts = dashboardService.restockAlerts(999L);

        assertEquals(1, alerts.size());
        RestockAlert alert = alerts.get(0);
        assertEquals(10L, alert.productId());
        assertEquals("SKU-1", alert.sku());
        assertEquals("Tornillo", alert.productoNombre());
        assertEquals(1L, alert.sucursalId());
        assertEquals("Norte", alert.sucursalNombre());
        assertEquals(0, alert.cantidadActual().compareTo(new BigDecimal("2")));
        assertEquals(0, alert.stockMinimo().compareTo(new BigDecimal("10")));
    }

    // --- branch-comparison (RF-30) --------------------------------------------------

    @Test
    void branchComparison_noConsultaElAlcanceDelUsuario_esSiempreGlobal() {
        when(sucursalRepository.comparativaPorSucursal(any(), any(), eq(TipoMovimiento.RETIRO), any()))
                .thenReturn(List.of());

        dashboardService.branchComparison();

        verifyNoInteractions(currentUser);
    }

    @Test
    void branchComparison_usaMesActualParaVentasYUltimos30DiasParaRotacion() {
        ArgumentCaptor<Instant> ventasDesde = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> ventasHasta = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> rotacionDesde = ArgumentCaptor.forClass(Instant.class);
        when(sucursalRepository.comparativaPorSucursal(
                ventasDesde.capture(), ventasHasta.capture(), eq(TipoMovimiento.RETIRO), rotacionDesde.capture()))
                .thenReturn(List.of());

        Instant antes = Instant.now();
        dashboardService.branchComparison();

        YearMonth mesActual = YearMonth.now(ZoneOffset.UTC);
        assertEquals(mesActual.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(), ventasDesde.getValue());
        assertEquals(mesActual.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(), ventasHasta.getValue());
        assertTrue(rotacionDesde.getValue().isBefore(antes) || rotacionDesde.getValue().equals(antes));
        assertTrue(rotacionDesde.getValue().isAfter(antes.minusSeconds(30 * 86400L + 5)));
    }

    @Test
    void branchComparison_devuelveLoQueDaElRepositorioTalCual() {
        BranchComparisonRow fila = new BranchComparisonRow(1L, "Norte", new BigDecimal("1000"), new BigDecimal("50"));
        when(sucursalRepository.comparativaPorSucursal(any(), any(), any(), any())).thenReturn(List.of(fila));

        List<BranchComparisonRow> response = dashboardService.branchComparison();

        assertEquals(List.of(fila), response);
    }
}
