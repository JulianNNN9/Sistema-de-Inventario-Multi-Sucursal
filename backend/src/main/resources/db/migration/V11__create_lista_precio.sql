-- Módulo 3 · Fase A — tabla `lista_precio` (Sección 3, RF-15).
-- sucursal_id NULL = lista de precios global (aplicable a toda la red).
CREATE TABLE lista_precio (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL,
    sucursal_id BIGINT REFERENCES sucursal (id)
);

CREATE INDEX idx_lista_precio_sucursal_id ON lista_precio (sucursal_id);
