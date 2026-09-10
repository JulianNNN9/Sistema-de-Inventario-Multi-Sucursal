-- Módulo 2 · Fase A — tabla `orden_compra_detalle` (Sección 3, RF-09).
-- descuento en porcentaje (0-100).
CREATE TABLE orden_compra_detalle (
    id              BIGSERIAL PRIMARY KEY,
    orden_id        BIGINT        NOT NULL REFERENCES orden_compra (id),
    producto_id     BIGINT        NOT NULL REFERENCES producto (id),
    cantidad        NUMERIC(12, 2) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    descuento       NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    -- Sección 3: "descuento en porcentaje 0-100"
    CONSTRAINT chk_ocd_descuento CHECK (descuento >= 0 AND descuento <= 100)
);

CREATE INDEX idx_ocd_orden_id ON orden_compra_detalle (orden_id);
CREATE INDEX idx_ocd_producto_id ON orden_compra_detalle (producto_id);
