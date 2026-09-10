-- Módulo 2 · Fase A — tabla `proveedor` (Sección 3).
CREATE TABLE proveedor (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(160) NOT NULL,
    condiciones TEXT
);
