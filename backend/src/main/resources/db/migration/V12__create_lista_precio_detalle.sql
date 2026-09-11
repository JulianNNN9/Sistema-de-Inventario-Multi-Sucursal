-- Módulo 3 · Fase A — tabla `lista_precio_detalle` (Sección 3, RF-15).
CREATE TABLE lista_precio_detalle (
    id          BIGSERIAL PRIMARY KEY,
    lista_id    BIGINT        NOT NULL REFERENCES lista_precio (id),
    producto_id BIGINT        NOT NULL REFERENCES producto (id),
    precio      NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uq_lpd_lista_producto UNIQUE (lista_id, producto_id)
);
