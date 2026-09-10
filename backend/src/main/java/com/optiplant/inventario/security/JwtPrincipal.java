package com.optiplant.inventario.security;

import com.optiplant.inventario.usuario.entity.Rol;

/**
 * Principal autenticado, reconstruido en cada petición a partir de los claims
 * del JWT (sin round-trip a base de datos). {@code sucursalId} es {@code null}
 * para {@link Rol#ADMIN_GENERAL}.
 */
public record JwtPrincipal(Long usuarioId, Long sucursalId, Rol rol) {
}
