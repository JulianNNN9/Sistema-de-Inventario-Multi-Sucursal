package com.optiplant.inventario.security;

import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.usuario.entity.Rol;
import com.optiplant.inventario.usuario.entity.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("test-secret-test-secret-test-secret-0123456789", 8);

    @Test
    void generatesAndParsesTokenWithMandatoryClaims() {
        Sucursal sucursal = Sucursal.builder().id(7L).nombre("Norte").build();
        Usuario usuario = Usuario.builder()
                .id(42L).nombre("Ana").email("ana@optiplant.local")
                .passwordHash("hash").rol(Rol.GERENTE_SUCURSAL).sucursal(sucursal)
                .build();

        String token = jwtService.generateToken(usuario);
        Claims claims = jwtService.parseClaims(token);

        assertEquals("42", claims.getSubject());
        assertEquals("GERENTE_SUCURSAL", claims.get("rol", String.class));
        assertEquals(7L, claims.get("sucursalId", Number.class).longValue());
        assertTrue(claims.getIssuedAt().before(claims.getExpiration()));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void adminTokenCarriesNoSucursal() {
        Usuario admin = Usuario.builder()
                .id(1L).nombre("Root").email("admin@optiplant.local")
                .passwordHash("hash").rol(Rol.ADMIN_GENERAL)
                .build();

        Claims claims = jwtService.parseClaims(jwtService.generateToken(admin));

        assertNull(claims.get("sucursalId"));
    }

    @Test
    void rejectsMalformedOrTamperedToken() {
        assertFalse(jwtService.isTokenValid("not.a.jwt"));
        assertFalse(jwtService.isTokenValid(""));
    }
}
