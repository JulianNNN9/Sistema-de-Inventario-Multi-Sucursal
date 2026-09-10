package com.optiplant.inventario.usuario.dto;

/** Respuesta de los endpoints de {@code /api/v1/users}. Nunca expone el hash. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Long sucursalId,
        String sucursalNombre
) {
}
