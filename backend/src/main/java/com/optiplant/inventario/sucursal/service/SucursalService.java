package com.optiplant.inventario.sucursal.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.sucursal.dto.SucursalRequest;
import com.optiplant.inventario.sucursal.dto.SucursalResponse;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de sucursales (Fase 0.D). El alta de una sucursal es el
 * único punto por el que se incorpora un nodo nuevo a la red (RNF-03).
 */
@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalRepository sucursalRepository;

    @Transactional
    public SucursalResponse crear(SucursalRequest request) {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(request.nombre());
        sucursal.setCiudad(request.ciudad());
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional(readOnly = true)
    public PageResponse<SucursalResponse> listar(Pageable pageable) {
        return PageResponse.from(sucursalRepository.findAll(pageable).map(this::toResponse));
    }

    /** Reutilizada por el resto de módulos para resolver referencias a sucursal. */
    @Transactional(readOnly = true)
    public Sucursal getEntityById(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal", id));
    }

    private SucursalResponse toResponse(Sucursal sucursal) {
        return new SucursalResponse(sucursal.getId(), sucursal.getNombre(), sucursal.getCiudad());
    }
}
