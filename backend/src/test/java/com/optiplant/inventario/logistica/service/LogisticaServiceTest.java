package com.optiplant.inventario.logistica.service;

import com.optiplant.inventario.logistica.dto.ComplianceReportResponse;
import com.optiplant.inventario.logistica.dto.ComplianceReportRow;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La agregación en sí (AVG/EXTRACT en SQL nativo) ya fue validada extremo a
 * extremo con datos reales contra Postgres (Módulo 5, Fase A/B/C). Estas
 * pruebas cubren lo que sí es responsabilidad de {@link LogisticaService}: que
 * el mapeo fila-a-respuesta conserve los agregados sin alterarlos (incluida la
 * desviación nula) y que los filtros lleguen intactos al repositorio.
 */
@ExtendWith(MockitoExtension.class)
class LogisticaServiceTest {

    @Mock
    private TransferenciaRepository transferenciaRepository;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private LogisticaService logisticaService;

    @BeforeEach
    void adminVeTodasLasSucursales() {
        when(currentUser.isAdmin()).thenReturn(true);
    }

    private ComplianceReportRow fila(Long sucursalId, String sucursal, String transportista,
                                      Long cantidad, Double desviacion) {
        ComplianceReportRow row = mock(ComplianceReportRow.class);
        when(row.getSucursalOrigenId()).thenReturn(sucursalId);
        when(row.getSucursalOrigenNombre()).thenReturn(sucursal);
        when(row.getTransportista()).thenReturn(transportista);
        when(row.getCantidad()).thenReturn(cantidad);
        when(row.getDesviacionPromedioHoras()).thenReturn(desviacion);
        return row;
    }

    @Test
    void complianceReport_conservaLosAgregadosDeCadaFilaSinAlterarlos() {
        List<ComplianceReportRow> filas = List.of(
                fila(1L, "Sucursal Norte", "Transportes XYZ", 3L, 4.5),
                fila(2L, "Sucursal Sur", "Y", 1L, -12.75)
        );
        when(transferenciaRepository.complianceReport(null, null)).thenReturn(filas);

        List<ComplianceReportResponse> response = logisticaService.complianceReport(null, null);

        assertEquals(2, response.size());

        ComplianceReportResponse primera = response.get(0);
        assertEquals(1L, primera.sucursalOrigenId());
        assertEquals("Sucursal Norte", primera.sucursalOrigenNombre());
        assertEquals("Transportes XYZ", primera.transportista());
        assertEquals(3L, primera.cantidadTransferencias());
        assertEquals(4.5, primera.desviacionPromedioHoras());

        ComplianceReportResponse segunda = response.get(1);
        assertEquals(-12.75, segunda.desviacionPromedioHoras());
    }

    @Test
    void complianceReport_rutaSinLlegadas_conservaDesviacionNula() {
        List<ComplianceReportRow> filas = List.of(fila(1L, "Sucursal Norte", "Nuevo Transportista", 2L, null));
        when(transferenciaRepository.complianceReport(null, null)).thenReturn(filas);

        List<ComplianceReportResponse> response = logisticaService.complianceReport(null, null);

        assertEquals(1, response.size());
        assertNull(response.get(0).desviacionPromedioHoras());
        assertEquals(2L, response.get(0).cantidadTransferencias());
    }

    @Test
    void complianceReport_propagaFiltrosSinModificarlosAlRepositorio() {
        when(transferenciaRepository.complianceReport(eq(1L), eq("Transportes XYZ")))
                .thenReturn(List.of());

        logisticaService.complianceReport(1L, "Transportes XYZ");

        verify(transferenciaRepository).complianceReport(eq(1L), eq("Transportes XYZ"));
    }

    @Test
    void complianceReport_sinFiltros_pasaNullAlRepositorioParaElCoalesceEnSql() {
        when(transferenciaRepository.complianceReport(isNull(), isNull())).thenReturn(List.of());

        logisticaService.complianceReport(null, null);

        verify(transferenciaRepository).complianceReport(isNull(), isNull());
    }

    @Test
    void complianceReport_sinResultados_devuelveListaVacia() {
        when(transferenciaRepository.complianceReport(null, null)).thenReturn(List.of());

        List<ComplianceReportResponse> response = logisticaService.complianceReport(null, null);

        assertTrue(response.isEmpty());
    }
}
