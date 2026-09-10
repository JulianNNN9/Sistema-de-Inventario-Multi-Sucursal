-- Módulo 0 · Fase 0.B — tabla `usuario` (Sección 3 del roadmap)
CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol           VARCHAR(30)  NOT NULL,
    sucursal_id   BIGINT       REFERENCES sucursal (id),
    CONSTRAINT chk_usuario_rol
        CHECK (rol IN ('ADMIN_GENERAL', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO')),
    -- sucursal_id es NULL únicamente para ADMIN_GENERAL (Sección 3)
    CONSTRAINT chk_usuario_sucursal_por_rol
        CHECK (rol = 'ADMIN_GENERAL' OR sucursal_id IS NOT NULL)
);

CREATE INDEX idx_usuario_sucursal_id ON usuario (sucursal_id);
