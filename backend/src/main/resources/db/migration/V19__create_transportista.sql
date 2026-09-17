-- Módulo 4 · catálogo de transportistas usados al despachar una transferencia
-- (antes era texto libre en transferencia.transportista).
CREATE TABLE transportista (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE
);

-- transferencia.transportista pasa de texto libre a llave foránea real hacia
-- el catálogo anterior. Si ya había transferencias con el texto poblado, se
-- da de alta cada nombre distinto en el catálogo (por si aún no existía) y se
-- resuelve el id correspondiente antes de eliminar la columna de texto.
ALTER TABLE transferencia
    ADD COLUMN transportista_id BIGINT REFERENCES transportista (id);

INSERT INTO transportista (nombre)
SELECT DISTINCT transportista
FROM transferencia
WHERE transportista IS NOT NULL
ON CONFLICT (nombre) DO NOTHING;

UPDATE transferencia t
SET transportista_id = tr.id
FROM transportista tr
WHERE tr.nombre = t.transportista;

ALTER TABLE transferencia
    DROP COLUMN transportista;
