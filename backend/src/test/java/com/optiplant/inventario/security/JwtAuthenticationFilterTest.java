package com.optiplant.inventario.security;

import com.optiplant.inventario.usuario.entity.Rol;
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica la comparación de {@code tokenVersion} en {@link JwtAuthenticationFilter}
 * (mitigación de robo/fuga de JWT): un token firmado y no expirado, pero con una
 * versión desactualizada respecto al usuario en base de datos (p. ej. porque hizo
 * logout en otra sesión), debe rechazarse igual que un token inválido.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private final JwtService jwtService =
            new JwtService("test-secret-test-secret-test-secret-0123456789", 8);

    @Mock
    private UsuarioRepository usuarioRepository;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaCuandoElTokenVersionCoincideConElUsuario() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Usuario usuario = Usuario.builder()
                .id(1L).nombre("Ana").email("ana@optiplant.local")
                .passwordHash("hash").rol(Rol.OPERADOR_INVENTARIO).tokenVersion(0)
                .build();
        String token = jwtService.generateToken(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(requestWithToken(token), new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertInstanceOf(JwtPrincipal.class, authentication.getPrincipal());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void rechazaTokenConTokenVersionDesactualizado() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Usuario usuario = Usuario.builder()
                .id(1L).nombre("Ana").email("ana@optiplant.local")
                .passwordHash("hash").rol(Rol.OPERADOR_INVENTARIO).tokenVersion(0)
                .build();
        // Token emitido cuando tokenVersion era 0...
        String token = jwtService.generateToken(usuario);
        // ...pero el usuario hizo logout después: su tokenVersion en BD ya avanzó a 1.
        usuario.setTokenVersion(1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(requestWithToken(token), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // Sin autenticar, no 500: la cadena sigue y el AuthenticationEntryPoint
        // responde 401 si el endpoint la exige, igual que un token inválido.
        verify(chain).doFilter(any(), any());
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
