-- Módulo 2 · Fase A — tabla `orden_compra` (Sección 3, RF-08). Inmutable salvo
-- transición de estado; sin DELETE físico (regla global 6).
CREATE TABLE orden_compra (
    id           BIGSERIAL PRIMARY KEY,
    proveedor_id BIGINT      NOT NULL REFERENCES proveedor (id),
    sucursal_id  BIGINT      NOT NULL REFERENCES sucursal (id),
    fecha        TIMESTAMP WITH TIME ZONE NOT NULL,
    estado       VARCHAR(20) NOT NULL,
    plazo_pago   VARCHAR(60),
    CONSTRAINT chk_orden_compra_estado
        CHECK (estado IN ('PENDIENTE', 'RECIBIDA', 'CANCELADA'))
);

CREATE INDEX idx_orden_compra_proveedor_id ON orden_compra (proveedor_id);
CREATE INDEX idx_orden_compra_sucursal_id ON orden_compra (sucursal_id);
