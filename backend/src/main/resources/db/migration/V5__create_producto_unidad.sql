-- Módulo 1 · Fase A — tabla `producto_unidad` (Sección 3, RF-06).
-- factor_conversion = cuántas unidad_medida_base equivalen a 1 de esta unidad.
CREATE TABLE producto_unidad (
    id                BIGSERIAL PRIMARY KEY,
    producto_id       BIGINT        NOT NULL REFERENCES producto (id),
    nombre_unidad     VARCHAR(30)   NOT NULL,
    factor_conversion NUMERIC(12, 4) NOT NULL
);

CREATE INDEX idx_producto_unidad_producto_id ON producto_unidad (producto_id);
