-- Módulo 4 · catálogo de transportistas usados al despachar una transferencia
-- (antes era texto libre en transferencia.transportista).
CREATE TABLE transportista (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE
);
