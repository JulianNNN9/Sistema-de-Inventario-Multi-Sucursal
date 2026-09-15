package com.optiplant.inventario.transferencia.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.transferencia.dto.TransportistaRequest;
import com.optiplant.inventario.transferencia.dto.TransportistaResponse;
import com.optiplant.inventario.transferencia.entity.Transportista;
import com.optiplant.inventario.transferencia.repository.TransportistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo de transportistas (Módulo 4). Alta abierta a los tres roles: quien
 * despacha una transferencia puede necesitar registrar un transportista nuevo
 * en el momento, y el catálogo queda disponible para todos de ahí en adelante.
 */
@Service
@RequiredArgsConstructor
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;

    @Transactional
    public TransportistaResponse crear(TransportistaRequest request) {
        if (transportistaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ValidacionException("Ya existe un transportista con ese nombre");
        }
        Transportista transportista = Transportista.builder().nombre(request.nombre()).build();
        return toResponse(transportistaRepository.save(transportista));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransportistaResponse> listar(Pageable pageable) {
        return PageResponse.from(transportistaRepository.findAllByOrderByNombreAsc(pageable).map(this::toResponse));
    }

    private TransportistaResponse toResponse(Transportista transportista) {
        return new TransportistaResponse(transportista.getId(), transportista.getNombre());
    }
}
