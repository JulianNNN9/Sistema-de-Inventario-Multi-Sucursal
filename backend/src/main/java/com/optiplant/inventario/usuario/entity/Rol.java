package com.optiplant.inventario.usuario.entity;

/**
 * Roles del sistema (Sección 3 y matriz de autorización de la Sección 4.2).
 */
public enum Rol {
    ADMIN_GENERAL,
    GERENTE_SUCURSAL,
    OPERADOR_INVENTARIO;

    /** Nombre en español natural, para usar en mensajes dirigidos al usuario final. */
    public String etiqueta() {
        return switch (this) {
            case ADMIN_GENERAL -> "administrador general";
            case GERENTE_SUCURSAL -> "gerente de sucursal";
            case OPERADOR_INVENTARIO -> "operador de inventario";
        };
    }
}
