-- Módulo 0 · Fase 0.B — tabla `sucursal` (Sección 3 del roadmap)
CREATE TABLE sucursal (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    ciudad VARCHAR(120)
);
