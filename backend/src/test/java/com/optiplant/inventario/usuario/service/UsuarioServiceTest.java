package com.optiplant.inventario.usuario.service;

import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.sucursal.service.SucursalService;
import com.optiplant.inventario.usuario.dto.UsuarioRequest;
import com.optiplant.inventario.usuario.dto.UsuarioResponse;
import com.optiplant.inventario.usuario.entity.Rol;
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void rechazaEmailDuplicado() {
        when(usuarioRepository.existsByEmail("ana@optiplant.local")).thenReturn(true);
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "ana@optiplant.local", "password1", Rol.OPERADOR_INVENTARIO, 1L);

        assertThrows(ValidacionException.class, () -> usuarioService.crear(request));
    }

    @Test
    void adminNoPuedeTenerSucursal() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        UsuarioRequest request = new UsuarioRequest(
                "Root", "root@optiplant.local", "password1", Rol.ADMIN_GENERAL, 5L);

        assertThrows(ValidacionException.class, () -> usuarioService.crear(request));
    }

    @Test
    void rolDeSucursalRequiereSucursalId() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "ana@optiplant.local", "password1", Rol.OPERADOR_INVENTARIO, null);

        assertThrows(ValidacionException.class, () -> usuarioService.crear(request));
    }

    @Test
    void creaOperadorConSucursalYHasheaPassword() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(sucursalService.getEntityById(1L))
                .thenReturn(Sucursal.builder().id(1L).nombre("Norte").build());
        when(passwordEncoder.encode("password1")).thenReturn("hashed-pw");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UsuarioRequest request = new UsuarioRequest(
                "Ana", "ana@optiplant.local", "password1", Rol.OPERADOR_INVENTARIO, 1L);
        UsuarioResponse response = usuarioService.crear(request);

        assertEquals(10L, response.id());
        assertEquals("Norte", response.sucursalNombre());
        assertEquals("OPERADOR_INVENTARIO", response.rol());
        verify(passwordEncoder).encode("password1");
    }

    @Test
    void creaAdminSinSucursal() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-pw");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioRequest request = new UsuarioRequest(
                "Root", "root@optiplant.local", "password1", Rol.ADMIN_GENERAL, null);
        UsuarioResponse response = usuarioService.crear(request);

        assertNull(response.sucursalId());
        assertNull(response.sucursalNombre());
    }
}
