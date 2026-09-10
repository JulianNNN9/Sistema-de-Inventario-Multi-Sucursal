-- Módulo 1 · Fase A — tabla `producto` (Sección 3). Catálogo común a toda la red (SUP-03).
CREATE TABLE producto (
    id                 BIGSERIAL PRIMARY KEY,
    sku                VARCHAR(60)  NOT NULL UNIQUE,
    nombre             VARCHAR(160) NOT NULL,
    unidad_medida_base VARCHAR(30)  NOT NULL
);
