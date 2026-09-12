-- Módulo 4 · Fase A — tabla `transferencia` (Sección 3, RF-17..RF-21).
-- 8 estados finales posibles según el diagrama de actividad. Inmutable salvo
-- transición de estado; sin DELETE físico (regla global 6).
CREATE TABLE transferencia (
    id                     BIGSERIAL PRIMARY KEY,
    producto_id            BIGINT        NOT NULL REFERENCES producto (id),
    sucursal_origen_id     BIGINT        NOT NULL REFERENCES sucursal (id),
    sucursal_destino_id    BIGINT        NOT NULL REFERENCES sucursal (id),
    cantidad_solicitada    NUMERIC(12, 2) NOT NULL,
    cantidad_enviada       NUMERIC(12, 2),
    cantidad_recibida      NUMERIC(12, 2),
    costo                  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado                 VARCHAR(30)   NOT NULL,
    urgencia               VARCHAR(20)   NOT NULL,
    transportista          VARCHAR(120),
    fecha_estimada_llegada TIMESTAMP WITH TIME ZONE,
    fecha_real_llegada     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_transferencia_estado CHECK (estado IN (
        'PENDIENTE', 'RECHAZADA', 'EN_TRANSITO', 'COMPLETADA', 'CON_FALTANTES',
        'REENVIO_SOLICITADO', 'CERRADA_AJUSTE', 'CERRADA_RECLAMACION')),
    CONSTRAINT chk_transferencia_urgencia CHECK (urgencia IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT chk_transferencia_sucursales_distintas
        CHECK (sucursal_origen_id <> sucursal_destino_id)
);

-- RNF-01: índice obligatorio + índices de FK para la bandeja por sucursal.
CREATE INDEX idx_transferencia_estado_origen_destino
    ON transferencia (estado, sucursal_origen_id, sucursal_destino_id);
CREATE INDEX idx_transferencia_origen ON transferencia (sucursal_origen_id);
CREATE INDEX idx_transferencia_destino ON transferencia (sucursal_destino_id);
CREATE INDEX idx_transferencia_producto ON transferencia (producto_id);
