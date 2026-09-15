-- Módulo 4 (Sección 3, RF-21): descripción del incidente al resolver un
-- faltante (antes solo vivía embebida en el comentario del evento, difícil
-- de previsualizar) y enlace a la transferencia original cuando esta es un
-- reenvío por faltante de otra.
ALTER TABLE transferencia
    ADD COLUMN detalle_resolucion TEXT,
    ADD COLUMN reenvio_de_id BIGINT REFERENCES transferencia (id);
