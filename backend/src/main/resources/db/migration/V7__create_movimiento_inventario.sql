-- Módulo 1 · Fase A — tabla `movimiento_inventario` (Sección 3, RF-07).
-- Historial auditable append-only: sin DELETE físico (regla global 6).
CREATE TABLE movimiento_inventario (
    id             BIGSERIAL PRIMARY KEY,
    inventario_id  BIGINT        NOT NULL REFERENCES inventario_sucursal (id),
    tipo           VARCHAR(10)   NOT NULL,
    motivo         VARCHAR(30)   NOT NULL,
    cantidad       NUMERIC(12, 2) NOT NULL,
    fecha          TIMESTAMP WITH TIME ZONE NOT NULL,
    responsable_id BIGINT        NOT NULL REFERENCES usuario (id),
    CONSTRAINT chk_movimiento_tipo CHECK (tipo IN ('INGRESO', 'RETIRO')),
    CONSTRAINT chk_movimiento_motivo CHECK (motivo IN (
        'COMPRA', 'DEVOLUCION', 'AJUSTE', 'VENTA', 'MERMA',
        'TRANSFERENCIA_SALIDA', 'TRANSFERENCIA_ENTRADA'))
);

-- RNF-01: índice obligatorio para las consultas de historial y del dashboard.
CREATE INDEX idx_movimiento_inventario_inv_fecha ON movimiento_inventario (inventario_id, fecha);
