package com.optiplant.inventario.transferencia.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
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
import com.optiplant.inventario.transferencia.dto.TransferEventResponse;
import com.optiplant.inventario.transferencia.dto.TransferRequest;
import com.optiplant.inventario.transferencia.dto.TransferResponse;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.entity.Transferencia;
import com.optiplant.inventario.transferencia.entity.TransferenciaEvento;
import com.optiplant.inventario.transferencia.repository.TransferenciaEventoRepository;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Ciclo de vida de una transferencia entre sucursales (RF-17..RF-21, RN-02).
 * Sigue exactamente el diagrama de actividad del roadmap: no se inventan
 * estados intermedios adicionales a los 8 fijados en la Sección 3.
 */
@Service
@RequiredArgsConstructor
public class TransferenciaService {

    /**
     * El modelo de estados no tiene "APROBADA" propio (Sección 3): al aprobar,
     * la transferencia permanece en PENDIENTE ("disponible para preparación").
     * Se marca con este evento para poder exigirlo antes del despacho.
     */
    private static final String EVENTO_APROBADA = "Solicitud aprobada por la sucursal origen; disponible para despacho";
    private static final String EVENTO_RECHAZADA = "Solicitud rechazada por la sucursal origen";

    private final TransferenciaRepository transferenciaRepository;
    private final TransferenciaEventoRepository transferenciaEventoRepository;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final InventarioService inventarioService;
    private final CurrentUser currentUser;

    @Transactional
    public TransferResponse solicitar(TransferRequest request) {
        Long destinoId = resolverDestino(request.sucursalDestinoId());
        Producto producto = productoService.getEntityById(request.productId());
        Sucursal origen = sucursalService.getEntityById(request.sucursalOrigenId());
        Sucursal destino = sucursalService.getEntityById(destinoId);

        if (origen.getId().equals(destino.getId())) {
            throw new ValidacionException(
                    "campo 'sucursalOrigenId': no puede coincidir con la sucursal destino");
        }

        Transferencia transferencia = transferenciaRepository.save(Transferencia.builder()
                .producto(producto)
                .sucursalOrigen(origen)
                .sucursalDestino(destino)
                .cantidadSolicitada(request.cantidad())
                .estado(EstadoTransferencia.PENDIENTE)
                .urgencia(request.urgencia())
                .build());
        registrarEvento(transferencia, EstadoTransferencia.PENDIENTE, "Solicitud creada");
        return toResponse(transferencia);
    }

    @Transactional
    public TransferResponse aprobar(Long id, ApproveRequest request) {
        Transferencia transferencia = getEntity(id);
        currentUser.assertPuedeOperarSobreSucursal(transferencia.getSucursalOrigen().getId());
        exigirEstado(transferencia, EstadoTransferencia.PENDIENTE, "aprobarse o rechazarse");

        if (Boolean.TRUE.equals(request.aprobado())) {
            if (transferenciaEventoRepository.existsByTransferenciaIdAndComentario(id, EVENTO_APROBADA)) {
                throw new TransferenciaInvalidaException("La transferencia ya fue aprobada");
            }
            registrarEvento(transferencia, EstadoTransferencia.PENDIENTE, EVENTO_APROBADA);
        } else {
            transferencia.setEstado(EstadoTransferencia.RECHAZADA);
            transferenciaRepository.save(transferencia);
            registrarEvento(transferencia, EstadoTransferencia.RECHAZADA, EVENTO_RECHAZADA);
        }
        return toResponse(transferencia);
    }

    @Transactional
    public TransferResponse despachar(Long id, DispatchRequest request) {
        Transferencia transferencia = getEntity(id);
        currentUser.assertPuedeOperarSobreSucursal(transferencia.getSucursalOrigen().getId());
        exigirEstado(transferencia, EstadoTransferencia.PENDIENTE, "prepararse");

        if (!transferenciaEventoRepository.existsByTransferenciaIdAndComentario(id, EVENTO_APROBADA)) {
            throw new TransferenciaInvalidaException(
                    "La transferencia no puede prepararse porque aún no ha sido aprobada");
        }

        inventarioService.registrarSalidaPorTransferencia(
                transferencia.getProducto(), transferencia.getSucursalOrigen(),
                request.cantidadEnviada(), currentUser.usuarioId());

        transferencia.setCantidadEnviada(request.cantidadEnviada());
        transferencia.setTransportista(request.transportista());
        transferencia.setFechaEstimadaLlegada(request.fechaEstimadaLlegada());
        transferencia.setEstado(EstadoTransferencia.EN_TRANSITO);
        transferenciaRepository.save(transferencia);
        registrarEvento(transferencia, EstadoTransferencia.EN_TRANSITO,
                "Despachada con " + request.transportista());
        return toResponse(transferencia);
    }

    @Transactional
    public TransferResponse recibir(Long id, ReceiveRequest request) {
        Transferencia transferencia = getEntity(id);
        currentUser.assertPuedeOperarSobreSucursal(transferencia.getSucursalDestino().getId());
        exigirEstado(transferencia, EstadoTransferencia.EN_TRANSITO, "recibirse");

        BigDecimal enviada = transferencia.getCantidadEnviada();
        BigDecimal recibida = request.cantidadRecibida();
        if (recibida.compareTo(enviada) > 0) {
            throw new ValidacionException(
                    "campo 'cantidadRecibida': no puede superar la cantidad enviada (" + enviada + ")");
        }

        if (recibida.signum() > 0) {
            inventarioService.registrarIngresoPorTransferencia(
                    transferencia.getProducto(), transferencia.getSucursalDestino(),
                    recibida, currentUser.usuarioId());
        }

        transferencia.setCantidadRecibida(recibida);
        transferencia.setFechaRealLlegada(Instant.now());

        if (recibida.compareTo(enviada) == 0) {
            transferencia.setEstado(EstadoTransferencia.COMPLETADA);
            transferenciaRepository.save(transferencia);
            registrarEvento(transferencia, EstadoTransferencia.COMPLETADA, "Recepción completa");
        } else {
            transferencia.setEstado(EstadoTransferencia.CON_FALTANTES);
            transferenciaRepository.save(transferencia);
            registrarEvento(transferencia, EstadoTransferencia.CON_FALTANTES,
                    "Recepción parcial: faltan " + enviada.subtract(recibida) + " unidades");
        }
        return toResponse(transferencia);
    }

    /**
     * RF-21. REENVIO genera una nueva {@code Transferencia} PENDIENTE por la
     * diferencia; AJUSTE y RECLAMACION solo cierran el registro. AJUSTE no
     * genera un movimiento de inventario adicional: el roadmap no detalla su
     * mecánica y el faltante ya quedó reflejado (origen descontó lo enviado,
     * destino solo sumó lo recibido).
     */
    @Transactional
    public TransferResponse resolver(Long id, ResolveRequest request) {
        Transferencia transferencia = getEntity(id);
        currentUser.assertPuedeOperarSobreSucursal(transferencia.getSucursalDestino().getId());
        exigirEstado(transferencia, EstadoTransferencia.CON_FALTANTES, "admitir tratamiento de faltante");

        switch (request.tratamiento()) {
            case REENVIO -> {
                BigDecimal faltante = transferencia.getCantidadEnviada()
                        .subtract(transferencia.getCantidadRecibida());
                Transferencia reenvio = transferenciaRepository.save(Transferencia.builder()
                        .producto(transferencia.getProducto())
                        .sucursalOrigen(transferencia.getSucursalOrigen())
                        .sucursalDestino(transferencia.getSucursalDestino())
                        .cantidadSolicitada(faltante)
                        .estado(EstadoTransferencia.PENDIENTE)
                        .urgencia(transferencia.getUrgencia())
                        .build());
                registrarEvento(reenvio, EstadoTransferencia.PENDIENTE,
                        "Reenvío por faltante de la transferencia #" + transferencia.getId());

                transferencia.setEstado(EstadoTransferencia.REENVIO_SOLICITADO);
                transferenciaRepository.save(transferencia);
                registrarEvento(transferencia, EstadoTransferencia.REENVIO_SOLICITADO,
                        "Reenvío solicitado: nueva transferencia #" + reenvio.getId());
            }
            case AJUSTE -> {
                transferencia.setEstado(EstadoTransferencia.CERRADA_AJUSTE);
                transferenciaRepository.save(transferencia);
                registrarEvento(transferencia, EstadoTransferencia.CERRADA_AJUSTE,
                        "Faltante cerrado por ajuste de inventario");
            }
            case RECLAMACION -> {
                transferencia.setEstado(EstadoTransferencia.CERRADA_RECLAMACION);
                transferenciaRepository.save(transferencia);
                registrarEvento(transferencia, EstadoTransferencia.CERRADA_RECLAMACION,
                        "Reclamación formal generada a la sucursal origen");
            }
        }
        return toResponse(transferencia);
    }

    /**
     * RF-23/RF-24 (Módulo 5): {@code sort} acepta {@code priority} (urgencia
     * ALTA&gt;MEDIA&gt;BAJA), {@code cost} (proxy: cantidad_solicitada desc, no hay
     * campo de costo en el modelo) o {@code time} (fecha_estimada_llegada asc);
     * cualquier otro valor no ordena.
     */
    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> listar(EstadoTransferencia estado, Long branchIdParam,
                                                 String sort, int page, int size) {
        Long branchId = currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
        Page<Transferencia> resultado = "priority".equals(sort)
                ? transferenciaRepository.searchOrderByPriority(estado, branchId, PageRequest.of(page, size))
                : transferenciaRepository.search(estado, branchId, PageRequest.of(page, size, resolverOrden(sort)));
        return PageResponse.from(resultado.map(this::toResponse));
    }

    private Sort resolverOrden(String sort) {
        if ("cost".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "cantidadSolicitada");
        }
        if ("time".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "fechaEstimadaLlegada");
        }
        return Sort.unsorted();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferEventResponse> eventos(Long transferenciaId, Pageable pageable) {
        Transferencia transferencia = getEntity(transferenciaId);
        if (!currentUser.isAdmin()
                && !Objects.equals(currentUser.sucursalId(), transferencia.getSucursalOrigen().getId())
                && !Objects.equals(currentUser.sucursalId(), transferencia.getSucursalDestino().getId())) {
            throw new RecursoNoEncontradoException("Transferencia", transferenciaId);
        }
        return PageResponse.from(transferenciaEventoRepository.findByTransferenciaId(transferenciaId, pageable)
                .map(this::toEventResponse));
    }

    private Long resolverDestino(Long sucursalDestinoIdFromRequest) {
        if (currentUser.isAdmin()) {
            if (sucursalDestinoIdFromRequest == null) {
                throw new ValidacionException("campo 'sucursalDestinoId': es obligatorio para ADMIN_GENERAL");
            }
            return sucursalDestinoIdFromRequest;
        }
        return currentUser.sucursalId();
    }

    private void exigirEstado(Transferencia transferencia, EstadoTransferencia esperado, String accion) {
        if (transferencia.getEstado() != esperado) {
            throw new TransferenciaInvalidaException(
                    "La transferencia está en estado " + transferencia.getEstado()
                            + " y no puede " + accion);
        }
    }

    private Transferencia getEntity(Long id) {
        return transferenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Transferencia", id));
    }

    private void registrarEvento(Transferencia transferencia, EstadoTransferencia estado, String comentario) {
        transferenciaEventoRepository.save(TransferenciaEvento.builder()
                .transferencia(transferencia)
                .estado(estado)
                .fecha(Instant.now())
                .comentario(comentario)
                .build());
    }

    private TransferResponse toResponse(Transferencia t) {
        return new TransferResponse(
                t.getId(),
                t.getProducto().getId(), t.getProducto().getSku(), t.getProducto().getNombre(),
                t.getSucursalOrigen().getId(), t.getSucursalOrigen().getNombre(),
                t.getSucursalDestino().getId(), t.getSucursalDestino().getNombre(),
                t.getCantidadSolicitada(), t.getCantidadEnviada(), t.getCantidadRecibida(),
                t.getEstado(), t.getUrgencia(), t.getTransportista(),
                t.getFechaEstimadaLlegada(), t.getFechaRealLlegada());
    }

    private TransferEventResponse toEventResponse(TransferenciaEvento evento) {
        return new TransferEventResponse(evento.getId(), evento.getEstado(), evento.getFecha(), evento.getComentario());
    }
}
