package com.optiplant.inventario.rebalanceo.service;

import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.rebalanceo.dto.RebalanceApproveRequest;
import com.optiplant.inventario.rebalanceo.dto.Sugerencia;
import com.optiplant.inventario.rebalanceo.strategy.RebalanceoStrategy;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.transferencia.dto.TransferRequest;
import com.optiplant.inventario.transferencia.dto.TransferResponse;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.entity.Urgencia;
import com.optiplant.inventario.transferencia.service.TransferenciaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RebalanceoServiceTest {

    @Mock
    private RebalanceoStrategy rebalanceoStrategy;
    @Mock
    private TransferenciaService transferenciaService;
    @Mock
    private InventarioSucursalRepository inventarioSucursalRepository;

    @InjectMocks
    private RebalanceoService rebalanceoService;

    private final Producto producto = Producto.builder().id(10L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("u").build();
    private final Sucursal sucursal = Sucursal.builder().id(2L).nombre("Sur").build();

    // --- sugerencias (RF-31) ---------------------------------------------------------

    @Test
    void sugerencias_delegaEnLaStrategyInyectada() {
        Sugerencia sugerencia = new Sugerencia(10L, "SKU-1", "Tornillo", new BigDecimal("5"),
                1L, "Norte", 2L, "Sur", Urgencia.MEDIA);
        when(rebalanceoStrategy.calcular()).thenReturn(List.of(sugerencia));

        List<Sugerencia> resultado = rebalanceoService.sugerencias();

        assertEquals(List.of(sugerencia), resultado);
    }

    // --- aprobar (RF-33) --------------------------------------------------------------

    @Test
    void aprobar_destinoInexistente_lanzaRecursoNoEncontrado() {
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 2L)).thenReturn(Optional.empty());

        RebalanceApproveRequest request = new RebalanceApproveRequest(10L, new BigDecimal("5"), 1L, 2L);

        assertThrows(RecursoNoEncontradoException.class, () -> rebalanceoService.aprobar(request));
    }

    @Test
    void aprobar_destinoEnCero_recalculaUrgenciaAltaIgnorandoElBody() {
        InventarioSucursal destino = InventarioSucursal.builder()
                .producto(producto).sucursal(sucursal)
                .cantidadActual(BigDecimal.ZERO).stockMinimo(new BigDecimal("10"))
                .build();
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 2L)).thenReturn(Optional.of(destino));
        when(transferenciaService.solicitar(any())).thenReturn(dummyResponse());

        rebalanceoService.aprobar(new RebalanceApproveRequest(10L, new BigDecimal("5"), 1L, 2L));

        ArgumentCaptor<TransferRequest> captor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferenciaService).solicitar(captor.capture());
        assertEquals(Urgencia.ALTA, captor.getValue().urgencia());
    }

    @Test
    void aprobar_destinoConAlgoDeStock_recalculaUrgenciaMedia() {
        InventarioSucursal destino = InventarioSucursal.builder()
                .producto(producto).sucursal(sucursal)
                .cantidadActual(new BigDecimal("3")).stockMinimo(new BigDecimal("10"))
                .build();
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 2L)).thenReturn(Optional.of(destino));
        when(transferenciaService.solicitar(any())).thenReturn(dummyResponse());

        rebalanceoService.aprobar(new RebalanceApproveRequest(10L, new BigDecimal("5"), 1L, 2L));

        ArgumentCaptor<TransferRequest> captor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferenciaService).solicitar(captor.capture());
        assertEquals(Urgencia.MEDIA, captor.getValue().urgencia());
    }

    @Test
    void aprobar_construyeElTransferRequestConLosDatosDelBody() {
        InventarioSucursal destino = InventarioSucursal.builder()
                .producto(producto).sucursal(sucursal)
                .cantidadActual(new BigDecimal("3")).stockMinimo(new BigDecimal("10"))
                .build();
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 2L)).thenReturn(Optional.of(destino));
        when(transferenciaService.solicitar(any())).thenReturn(dummyResponse());

        rebalanceoService.aprobar(new RebalanceApproveRequest(10L, new BigDecimal("7.5"), 1L, 2L));

        ArgumentCaptor<TransferRequest> captor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferenciaService).solicitar(captor.capture());
        TransferRequest enviado = captor.getValue();
        assertEquals(10L, enviado.productId());
        assertEquals(0, enviado.cantidad().compareTo(new BigDecimal("7.5")));
        assertEquals(1L, enviado.sucursalOrigenId());
        assertEquals(2L, enviado.sucursalDestinoId());
    }

    @Test
    void aprobar_devuelveLaRespuestaDeTransferenciaServiceTalCual() {
        InventarioSucursal destino = InventarioSucursal.builder()
                .producto(producto).sucursal(sucursal)
                .cantidadActual(new BigDecimal("3")).stockMinimo(new BigDecimal("10"))
                .build();
        when(inventarioSucursalRepository.findByProductoIdAndSucursalId(10L, 2L)).thenReturn(Optional.of(destino));
        TransferResponse respuesta = dummyResponse();
        when(transferenciaService.solicitar(any())).thenReturn(respuesta);

        TransferResponse resultado = rebalanceoService.aprobar(new RebalanceApproveRequest(10L, new BigDecimal("5"), 1L, 2L));

        assertEquals(respuesta, resultado);
    }

    private TransferResponse dummyResponse() {
        return new TransferResponse(99L, 10L, "SKU-1", "Tornillo", 1L, "Norte", 2L, "Sur",
                new BigDecimal("5"), null, null, BigDecimal.ZERO, EstadoTransferencia.PENDIENTE, Urgencia.MEDIA, null, null, null);
    }
}
