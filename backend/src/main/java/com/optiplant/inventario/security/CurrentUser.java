package com.optiplant.inventario.security;

import com.optiplant.inventario.usuario.entity.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acceso tipado al usuario autenticado desde la capa de Service. Centraliza la
 * verificación transversal de alcance por sucursal (RF-36, Sección 4.2).
 */
@Component
public class CurrentUser {

    public JwtPrincipal require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("No hay un usuario autenticado en el contexto");
        }
        return principal;
    }

    public Long usuarioId() {
        return require().usuarioId();
    }

    public Long sucursalId() {
        return require().sucursalId();
    }

    public Rol rol() {
        return require().rol();
    }

    public boolean isAdmin() {
        return rol() == Rol.ADMIN_GENERAL;
    }

    /**
     * Verifica que el recurso pertenece a la sucursal del usuario, salvo que sea
     * ADMIN_GENERAL. Regla transversal de escritura de la Sección 4.2.
     */
    public void assertPuedeOperarSobreSucursal(Long sucursalIdRecurso) {
        if (isAdmin()) {
            return;
        }
        if (sucursalIdRecurso == null || !sucursalIdRecurso.equals(sucursalId())) {
            throw new AccessDeniedException(
                    "No tiene permisos para operar sobre datos de otra sucursal");
        }
    }

    /**
     * Verifica únicamente que haya un usuario autenticado con uno de los tres
     * roles válidos, sin restringir por sucursal. La visibilidad de lectura
     * entre sucursales es de red completa para los tres roles (Sección 2.1,
     * RF-02); a diferencia de {@link #assertPuedeOperarSobreSucursal}, esta
     * verificación no aplica al alcance de escritura.
     */
    public void assertPuedeVerSucursal(Long sucursalId) {
        require();
    }
}
