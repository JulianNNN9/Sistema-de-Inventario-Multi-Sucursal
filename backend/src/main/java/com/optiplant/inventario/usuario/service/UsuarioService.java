package com.optiplant.inventario.usuario.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.usuario.dto.UsuarioRequest;
import com.optiplant.inventario.usuario.dto.UsuarioResponse;
import com.optiplant.inventario.usuario.dto.UsuarioUpdateRequest;
import com.optiplant.inventario.usuario.entity.Rol;
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de usuarios (Fase 0.D). Gestión restringida a
 * {@code ADMIN_GENERAL} (Sección 4.2, HU-18).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalService sucursalService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ValidacionException("campo 'email': ya existe un usuario con ese email");
        }
        Sucursal sucursal = resolverSucursal(request.rol(), request.sucursalId());

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setSucursal(sucursal);
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        Sucursal sucursal = resolverSucursal(request.rol(), request.sucursalId());

        usuario.setNombre(request.nombre());
        usuario.setRol(request.rol());
        usuario.setSucursal(sucursal);
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Pageable pageable) {
        return PageResponse.from(usuarioRepository.findAll(pageable).map(this::toResponse));
    }

    /**
     * Regla de negocio (Sección 3): {@code ADMIN_GENERAL} nunca lleva sucursal;
     * el resto de roles la exige y debe existir.
     */
    private Sucursal resolverSucursal(Rol rol, Long sucursalId) {
        if (rol == Rol.ADMIN_GENERAL) {
            if (sucursalId != null) {
                throw new ValidacionException(
                        "campo 'sucursalId': ADMIN_GENERAL no puede tener sucursal asignada");
            }
            return null;
        }
        if (sucursalId == null) {
            throw new ValidacionException(
                    "campo 'sucursalId': es obligatorio para el rol " + rol.name());
        }
        return sucursalService.getEntityById(sucursalId);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        Sucursal sucursal = usuario.getSucursal();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().name(),
                sucursal != null ? sucursal.getId() : null,
                sucursal != null ? sucursal.getNombre() : null);
    }
}
