-- Módulo 1 · Fase A — tabla `inventario_sucursal` (Sección 3).
-- Nivel de stock, stock mínimo y costo promedio ponderado por (producto, sucursal).
CREATE TABLE inventario_sucursal (
    id                       BIGSERIAL PRIMARY KEY,
    producto_id              BIGINT        NOT NULL REFERENCES producto (id),
    sucursal_id              BIGINT        NOT NULL REFERENCES sucursal (id),
    cantidad_actual          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    stock_minimo             NUMERIC(12, 2) NOT NULL DEFAULT 0,
    costo_promedio_ponderado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventario_producto_sucursal UNIQUE (producto_id, sucursal_id)
);
