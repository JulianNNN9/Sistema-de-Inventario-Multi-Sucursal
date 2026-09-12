package com.optiplant.inventario.transferencia.service;

import com.optiplant.inventario.common.exception.TransferenciaInvalidaException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.inventario.service.InventarioService;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.producto.service.ProductoService;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.transferencia.dto.ApproveRequest;
import com.optiplant.inventario.transferencia.dto.DispatchRequest;
import com.optiplant.inventario.transferencia.dto.ReceiveRequest;
import com.optiplant.inventario.transferencia.dto.ResolveRequest;
import com.optiplant.inventario.transferencia.dto.TransferRequest;
import com.optiplant.inventario.transferencia.dto.TransferResponse;
import com.optiplant.inventario.transferencia.dto.TratamientoFaltante;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.entity.Transferencia;
import com.optiplant.inventario.transferencia.entity.Urgencia;
import com.optiplant.inventario.transferencia.repository.TransferenciaEventoRepository;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock
    private TransferenciaRepository transferenciaRepository;
    @Mock
    private TransferenciaEventoRepository transferenciaEventoRepository;
    @Mock
    private ProductoService productoService;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private TransferenciaService transferenciaService;

    private final Producto producto = Producto.builder()
            .id(10L).sku("SKU-1").nombre("Tornillo").unidadMedidaBase("u").build();
    private final Sucursal origen = Sucursal.builder().id(1L).nombre("Norte").build();
    private final Sucursal destino = Sucursal.builder().id(2L).nombre("Sur").build();

    private Transferencia transferenciaEn(EstadoTransferencia estado) {
        return Transferencia.builder()
                .id(99L).producto(producto).sucursalOrigen(origen).sucursalDestino(destino)
                .cantidadSolicitada(new BigDecimal("20"))
                .estado(estado).urgencia(Urgencia.MEDIA)
                .build();
    }

    // --- solicitar (RF-17) --------------------------------------------------------

    @Test
    void solicitar_adminSinSucursalDestino_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(true);

        TransferRequest request = new TransferRequest(10L, new BigDecimal("5"), 1L, Urgencia.MEDIA, null);

        assertThrows(ValidacionException.class, () -> transferenciaService.solicitar(request));
    }

    @Test
    void solicitar_origenIgualDestino_lanzaValidacion() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(sucursalService.getEntityById(1L)).thenReturn(origen);

        TransferRequest request = new TransferRequest(10L, new BigDecimal("5"), 1L, Urgencia.MEDIA, null);

        assertThrows(ValidacionException.class, () -> transferenciaService.solicitar(request));
    }

    @Test
    void solicitar_ok_creaPendienteYRegistraEvento() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(productoService.getEntityById(10L)).thenReturn(producto);
        when(sucursalService.getEntityById(1L)).thenReturn(origen);
        when(sucursalService.getEntityById(2L)).thenReturn(destino);
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> {
            Transferencia t = i.getArgument(0);
            t.setId(99L);
            return t;
        });

        TransferRequest request = new TransferRequest(10L, new BigDecimal("5"), 1L, Urgencia.MEDIA, 2L);
        TransferResponse response = transferenciaService.solicitar(request);

        assertEquals(99L, response.id());
        assertEquals(EstadoTransferencia.PENDIENTE, response.estado());
        verify(transferenciaEventoRepository).save(any());
    }

    // --- aprobar (Sección 4.2) ------------------------------------------------------

    @Test
    void aprobar_estadoNoPendiente_lanzaInvalida() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.EN_TRANSITO)));

        assertThrows(TransferenciaInvalidaException.class,
                () -> transferenciaService.aprobar(99L, new ApproveRequest(true)));
    }

    @Test
    void aprobar_yaAprobada_lanzaInvalida() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.PENDIENTE)));
        when(transferenciaEventoRepository.existsByTransferenciaIdAndComentario(eq(99L), any())).thenReturn(true);

        assertThrows(TransferenciaInvalidaException.class,
                () -> transferenciaService.aprobar(99L, new ApproveRequest(true)));
    }

    @Test
    void aprobar_true_permaneceEnPendienteYRegistraEvento() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.PENDIENTE)));
        when(transferenciaEventoRepository.existsByTransferenciaIdAndComentario(eq(99L), any())).thenReturn(false);

        TransferResponse response = transferenciaService.aprobar(99L, new ApproveRequest(true));

        assertEquals(EstadoTransferencia.PENDIENTE, response.estado());
        verify(transferenciaRepository, never()).save(any());
        verify(transferenciaEventoRepository).save(any());
    }

    @Test
    void aprobar_false_marcaRechazada() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.PENDIENTE)));
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> i.getArgument(0));

        TransferResponse response = transferenciaService.aprobar(99L, new ApproveRequest(false));

        assertEquals(EstadoTransferencia.RECHAZADA, response.estado());
    }

    // --- despachar (RF-18, RF-19) ---------------------------------------------------

    @Test
    void despachar_sinAprobacionPrevia_lanzaInvalidaYNoDescuentaStock() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.PENDIENTE)));
        when(transferenciaEventoRepository.existsByTransferenciaIdAndComentario(eq(99L), any())).thenReturn(false);

        DispatchRequest request = new DispatchRequest(new BigDecimal("15"), "Transportes", Instant.now(), new BigDecimal("50000"));

        assertThrows(TransferenciaInvalidaException.class, () -> transferenciaService.despachar(99L, request));
        verify(inventarioService, never()).registrarSalidaPorTransferencia(any(), any(), any(), any());
    }

    @Test
    void despachar_ok_aplicaSalidaYPasaAEnTransito() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.PENDIENTE)));
        when(transferenciaEventoRepository.existsByTransferenciaIdAndComentario(eq(99L), any())).thenReturn(true);
        when(currentUser.usuarioId()).thenReturn(7L);
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> i.getArgument(0));

        DispatchRequest request = new DispatchRequest(
                new BigDecimal("15"), "Transportes XYZ", Instant.parse("2026-09-20T00:00:00Z"), new BigDecimal("85000"));
        TransferResponse response = transferenciaService.despachar(99L, request);

        assertEquals(EstadoTransferencia.EN_TRANSITO, response.estado());
        assertEquals(0, response.cantidadEnviada().compareTo(new BigDecimal("15")));
        assertEquals("Transportes XYZ", response.transportista());
        assertEquals(0, response.costo().compareTo(new BigDecimal("85000")));
        verify(inventarioService).registrarSalidaPorTransferencia(
                eq(producto), eq(origen), eq(new BigDecimal("15")), eq(7L));
    }

    // --- recibir (RF-20, RF-21) -------------------------------------------------------

    @Test
    void recibir_estadoNoEnTransito_lanzaInvalida() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.PENDIENTE)));

        assertThrows(TransferenciaInvalidaException.class,
                () -> transferenciaService.recibir(99L, new ReceiveRequest(new BigDecimal("5"))));
    }

    @Test
    void recibir_masQueLoEnviado_lanzaValidacion() {
        Transferencia t = transferenciaEn(EstadoTransferencia.EN_TRANSITO);
        t.setCantidadEnviada(new BigDecimal("10"));
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(t));

        assertThrows(ValidacionException.class,
                () -> transferenciaService.recibir(99L, new ReceiveRequest(new BigDecimal("15"))));
        verify(inventarioService, never()).registrarIngresoPorTransferencia(any(), any(), any(), any());
    }

    @Test
    void recibir_completa_marcaCompletadaYRegistraIngresoTotal() {
        Transferencia t = transferenciaEn(EstadoTransferencia.EN_TRANSITO);
        t.setCantidadEnviada(new BigDecimal("10"));
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(t));
        when(currentUser.usuarioId()).thenReturn(7L);
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> i.getArgument(0));

        TransferResponse response = transferenciaService.recibir(99L, new ReceiveRequest(new BigDecimal("10")));

        assertEquals(EstadoTransferencia.COMPLETADA, response.estado());
        verify(inventarioService).registrarIngresoPorTransferencia(
                eq(producto), eq(destino), eq(new BigDecimal("10")), eq(7L));
    }

    @Test
    void recibir_parcial_marcaConFaltantesYRegistraSoloLoRecibido() {
        Transferencia t = transferenciaEn(EstadoTransferencia.EN_TRANSITO);
        t.setCantidadEnviada(new BigDecimal("10"));
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(t));
        when(currentUser.usuarioId()).thenReturn(7L);
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> i.getArgument(0));

        TransferResponse response = transferenciaService.recibir(99L, new ReceiveRequest(new BigDecimal("6")));

        assertEquals(EstadoTransferencia.CON_FALTANTES, response.estado());
        verify(inventarioService).registrarIngresoPorTransferencia(
                eq(producto), eq(destino), eq(new BigDecimal("6")), eq(7L));
    }

    // --- resolver (RF-21) --------------------------------------------------------------

    @Test
    void resolver_estadoNoConFaltantes_lanzaInvalida() {
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(transferenciaEn(EstadoTransferencia.EN_TRANSITO)));

        assertThrows(TransferenciaInvalidaException.class,
                () -> transferenciaService.resolver(99L, new ResolveRequest(TratamientoFaltante.AJUSTE)));
    }

    @Test
    void resolver_reenvio_creaNuevaTransferenciaPendientePorElFaltante() {
        Transferencia t = transferenciaEn(EstadoTransferencia.CON_FALTANTES);
        t.setCantidadEnviada(new BigDecimal("10"));
        t.setCantidadRecibida(new BigDecimal("6"));
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(t));
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> {
            Transferencia arg = i.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(100L);
            }
            return arg;
        });

        transferenciaService.resolver(99L, new ResolveRequest(TratamientoFaltante.REENVIO));

        ArgumentCaptor<Transferencia> captor = ArgumentCaptor.forClass(Transferencia.class);
        verify(transferenciaRepository, times(2)).save(captor.capture());
        List<Transferencia> guardadas = captor.getAllValues();

        Transferencia reenvio = guardadas.get(0);
        assertEquals(EstadoTransferencia.PENDIENTE, reenvio.getEstado());
        assertEquals(0, reenvio.getCantidadSolicitada().compareTo(new BigDecimal("4")));

        assertEquals(EstadoTransferencia.REENVIO_SOLICITADO, t.getEstado());
    }

    @Test
    void resolver_ajuste_cierraSinTocarInventario() {
        Transferencia t = transferenciaEn(EstadoTransferencia.CON_FALTANTES);
        t.setCantidadEnviada(new BigDecimal("10"));
        t.setCantidadRecibida(new BigDecimal("6"));
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(t));
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> i.getArgument(0));

        TransferResponse response = transferenciaService.resolver(99L, new ResolveRequest(TratamientoFaltante.AJUSTE));

        assertEquals(EstadoTransferencia.CERRADA_AJUSTE, response.estado());
        verifyNoInteractions(inventarioService);
    }

    @Test
    void resolver_reclamacion_cierraConEstadoReclamacion() {
        Transferencia t = transferenciaEn(EstadoTransferencia.CON_FALTANTES);
        t.setCantidadEnviada(new BigDecimal("10"));
        t.setCantidadRecibida(new BigDecimal("6"));
        when(transferenciaRepository.findById(99L)).thenReturn(Optional.of(t));
        when(transferenciaRepository.save(any(Transferencia.class))).thenAnswer(i -> i.getArgument(0));

        TransferResponse response = transferenciaService.resolver(99L, new ResolveRequest(TratamientoFaltante.RECLAMACION));

        assertEquals(EstadoTransferencia.CERRADA_RECLAMACION, response.estado());
    }

    // --- listar / orden (RF-23, Módulo 5) -------------------------------------------

    private Page<Transferencia> paginaCon(Transferencia... transferencias) {
        return new PageImpl<>(List.of(transferencias));
    }

    @Test
    void listar_sortPriority_usaSearchOrderByPriorityYNoSearch() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(transferenciaRepository.searchOrderByPriority(eq(EstadoTransferencia.PENDIENTE), eq(1L), any(Pageable.class)))
                .thenReturn(paginaCon(transferenciaEn(EstadoTransferencia.PENDIENTE)));

        transferenciaService.listar(EstadoTransferencia.PENDIENTE, 1L, "priority", 0, 20);

        verify(transferenciaRepository).searchOrderByPriority(eq(EstadoTransferencia.PENDIENTE), eq(1L), any());
        verify(transferenciaRepository, never()).search(any(), any(), any());
    }

    @Test
    void listar_sortCost_ordenaPorCostoDescendente() {
        when(currentUser.isAdmin()).thenReturn(true);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(transferenciaRepository.search(any(), any(), pageableCaptor.capture()))
                .thenReturn(paginaCon());

        transferenciaService.listar(null, null, "cost", 0, 20);

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("costo");
        assertTrue(orden != null && orden.isDescending());
    }

    @Test
    void listar_sortTime_ordenaPorFechaEstimadaLlegadaAscendente() {
        when(currentUser.isAdmin()).thenReturn(true);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(transferenciaRepository.search(any(), any(), pageableCaptor.capture()))
                .thenReturn(paginaCon());

        transferenciaService.listar(null, null, "time", 0, 20);

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("fechaEstimadaLlegada");
        assertTrue(orden != null && orden.isAscending());
    }

    @Test
    void listar_sinSortReconocido_noAplicaOrden() {
        when(currentUser.isAdmin()).thenReturn(true);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(transferenciaRepository.search(any(), any(), pageableCaptor.capture()))
                .thenReturn(paginaCon());

        transferenciaService.listar(null, null, null, 0, 20);

        assertTrue(pageableCaptor.getValue().getSort().isUnsorted());
    }

    @Test
    void listar_noAdmin_ignoraBranchIdDelParametroYUsaSucursalDelUsuario() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.sucursalId()).thenReturn(1L);
        when(transferenciaRepository.search(isNull(), eq(1L), any())).thenReturn(paginaCon());

        // branchId=99 en el parámetro es de otra sucursal: debe ignorarse.
        transferenciaService.listar(null, 99L, null, 0, 20);

        verify(transferenciaRepository).search(isNull(), eq(1L), any());
    }

    @Test
    void listar_admin_respetaElBranchIdDelParametro() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(transferenciaRepository.search(isNull(), eq(2L), any())).thenReturn(paginaCon());

        transferenciaService.listar(null, 2L, null, 0, 20);

        verify(transferenciaRepository).search(isNull(), eq(2L), any());
    }

    @Test
    void listar_admin_sinBranchId_buscaEnTodasLasSucursales() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(transferenciaRepository.search(isNull(), isNull(), any())).thenReturn(paginaCon());

        transferenciaService.listar(null, null, null, 0, 20);

        verify(transferenciaRepository).search(isNull(), isNull(), any());
    }
}
