-- Módulo 3 · Fase A — tabla `venta` (Sección 3, RF-13, RF-16). Inmutable una vez
-- creada; sin DELETE físico (regla global 6).
CREATE TABLE venta (
    id          BIGSERIAL PRIMARY KEY,
    sucursal_id BIGINT        NOT NULL REFERENCES sucursal (id),
    usuario_id  BIGINT        NOT NULL REFERENCES usuario (id),
    fecha       TIMESTAMP WITH TIME ZONE NOT NULL,
    total       NUMERIC(12, 2) NOT NULL
);

-- RNF-01: índice obligatorio para el volumen de ventas del dashboard.
CREATE INDEX idx_venta_sucursal_fecha ON venta (sucursal_id, fecha);
