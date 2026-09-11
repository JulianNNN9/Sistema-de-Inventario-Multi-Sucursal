-- Módulo 4 · Fase A — tabla `transferencia_evento` (Sección 3, RF-22, RF-24).
-- Histórico append-only de cada cambio de estado (regla global 6).
CREATE TABLE transferencia_evento (
    id               BIGSERIAL PRIMARY KEY,
    transferencia_id BIGINT      NOT NULL REFERENCES transferencia (id),
    estado           VARCHAR(30) NOT NULL,
    fecha            TIMESTAMP WITH TIME ZONE NOT NULL,
    comentario       TEXT,
    CONSTRAINT chk_transferencia_evento_estado CHECK (estado IN (
        'PENDIENTE', 'RECHAZADA', 'EN_TRANSITO', 'COMPLETADA', 'CON_FALTANTES',
        'REENVIO_SOLICITADO', 'CERRADA_AJUSTE', 'CERRADA_RECLAMACION'))
);

CREATE INDEX idx_transferencia_evento_transferencia
    ON transferencia_evento (transferencia_id);
