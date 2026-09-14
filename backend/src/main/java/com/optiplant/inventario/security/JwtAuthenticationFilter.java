package com.optiplant.inventario.security;

import com.optiplant.inventario.usuario.entity.Rol;
import com.optiplant.inventario.usuario.entity.Usuario;
import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT dentro de la cadena de filtros de Spring Security
 * (patrón Chain of Responsibility, Sección 6). Reconstruye el
 * {@link JwtPrincipal} desde los claims; una petición sin token, con token
 * inválido, o con un {@code tokenVersion} desactualizado respecto al usuario
 * en base de datos (mitigación de robo/fuga de JWT, invalidado vía
 * {@code POST /api/v1/auth/logout}) simplemente continúa sin autenticación
 * (el {@code AuthenticationEntryPoint} responde 401 si el endpoint la exige).
 *
 * <p>Se instancia explícitamente en {@code SecurityConfig} (no es {@code @Component})
 * para evitar que Spring Boot lo registre además en la cadena de filtros del
 * servlet y se ejecute dos veces.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        if (SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isTokenValid(token)) {
            try {
                Claims claims = jwtService.parseClaims(token);
                Long usuarioId = Long.valueOf(claims.getSubject());
                Number tokenVersionClaim = claims.get("tokenVersion", Number.class);
                int tokenVersion = tokenVersionClaim != null ? tokenVersionClaim.intValue() : -1;

                Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                if (usuario != null && usuario.getTokenVersion() == tokenVersion) {
                    Rol rol = Rol.valueOf(claims.get("rol", String.class));
                    Number sucursalClaim = claims.get("sucursalId", Number.class);
                    Long sucursalId = sucursalClaim != null ? sucursalClaim.longValue() : null;

                    JwtPrincipal principal = new JwtPrincipal(usuarioId, sucursalId, rol);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // tokenVersion desactualizado (logout en otra sesión) o usuario ya no
                // existe → se continúa sin autenticación, igual que un token inválido.
            } catch (IllegalArgumentException ex) {
                // claims corruptos → se continúa sin autenticación
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
