-- Módulo 3 · Fase A — tabla `venta_detalle` (Sección 3, RF-13, RF-15).
CREATE TABLE venta_detalle (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT        NOT NULL REFERENCES venta (id),
    producto_id     BIGINT        NOT NULL REFERENCES producto (id),
    cantidad        NUMERIC(12, 2) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    descuento       NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    CONSTRAINT chk_vd_descuento CHECK (descuento >= 0 AND descuento <= 100)
);

CREATE INDEX idx_venta_detalle_venta_id ON venta_detalle (venta_id);
CREATE INDEX idx_venta_detalle_producto_id ON venta_detalle (producto_id);
