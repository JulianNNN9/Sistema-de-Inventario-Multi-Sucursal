package com.optiplant.inventario.security.auth;

import com.optiplant.inventario.common.exception.RecursoNoEncontradoException;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.security.JwtService;
import com.optiplant.inventario.security.auth.dto.LoginRequest;
import com.optiplant.inventario.security.auth.dto.LoginResponse;
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta el login: valida credenciales contra el {@code AuthenticationManager}
 * (que usa {@code UsuarioDetailsService} + {@code BCryptPasswordEncoder}) y emite
 * el JWT. También el logout: invalida de inmediato todos los tokens ya
 * emitidos para el usuario actual (mitigación de robo/fuga de JWT).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        String token = jwtService.generateToken(usuario);
        Long sucursalId = usuario.getSucursal() != null ? usuario.getSucursal().getId() : null;
        return new LoginResponse(token, usuario.getRol().name(), sucursalId, usuario.getNombre());
    }

    /**
     * Incrementa {@code tokenVersion} del usuario actual: cualquier token ya
     * emitido para él deja de ser válido de inmediato en
     * {@code JwtAuthenticationFilter}, aunque no haya expirado (RNF-02).
     */
    @Transactional
    public void logout() {
        Long usuarioId = currentUser.usuarioId();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);
    }
}
