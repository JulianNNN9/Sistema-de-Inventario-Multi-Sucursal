package com.optiplant.inventario.security;

import com.optiplant.inventario.usuario.entity.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link CurrentUser#assertPuedeVerSucursal} es de red completa para los tres
 * roles (Sección 2.1, RF-02/HU-02), a diferencia de
 * {@link CurrentUser#assertPuedeOperarSobreSucursal}, que sigue acotada a la
 * sucursal propia salvo ADMIN_GENERAL.
 */
class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long sucursalId, Rol rol) {
        JwtPrincipal principal = new JwtPrincipal(1L, sucursalId, rol);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
    }

    @Test
    void assertPuedeVerSucursal_operadorConsultaSucursalAjena_noLanza() {
        autenticarComo(1L, Rol.OPERADOR_INVENTARIO);

        assertDoesNotThrow(() -> currentUser.assertPuedeVerSucursal(2L));
    }

    @Test
    void assertPuedeVerSucursal_gerenteConsultaSucursalAjena_noLanza() {
        autenticarComo(1L, Rol.GERENTE_SUCURSAL);

        assertDoesNotThrow(() -> currentUser.assertPuedeVerSucursal(2L));
    }

    @Test
    void assertPuedeVerSucursal_sinAutenticar_lanzaAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> currentUser.assertPuedeVerSucursal(2L));
    }

    @Test
    void assertPuedeOperarSobreSucursal_operadorConSucursalAjena_siguelanzando() {
        autenticarComo(1L, Rol.OPERADOR_INVENTARIO);

        assertThrows(AccessDeniedException.class,
                () -> currentUser.assertPuedeOperarSobreSucursal(2L));
    }
}
