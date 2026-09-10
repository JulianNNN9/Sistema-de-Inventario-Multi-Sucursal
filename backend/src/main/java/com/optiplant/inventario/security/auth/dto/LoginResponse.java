package com.optiplant.inventario.security.auth.dto;

/** Respuesta de {@code POST /api/v1/auth/login} (Sección 4.1). */
public record LoginResponse(
        String token,
        String rol,
        Long sucursalId,
        String nombre
) {
}
