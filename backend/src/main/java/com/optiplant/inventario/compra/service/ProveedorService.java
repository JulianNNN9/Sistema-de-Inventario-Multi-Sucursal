package com.optiplant.inventario.compra.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.compra.dto.ProveedorRequest;
import com.optiplant.inventario.compra.dto.ProveedorResponse;
import com.optiplant.inventario.compra.entity.Proveedor;
import com.optiplant.inventario.compra.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        Proveedor proveedor = Proveedor.builder()
                .nombre(request.nombre())
                .condiciones(request.condiciones())
                .build();
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProveedorResponse> listar(Pageable pageable) {
        return PageResponse.from(proveedorRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public Proveedor getEntityById(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", id));
    }

    private ProveedorResponse toResponse(Proveedor proveedor) {
        return new ProveedorResponse(proveedor.getId(), proveedor.getNombre(), proveedor.getCondiciones());
    }
}
