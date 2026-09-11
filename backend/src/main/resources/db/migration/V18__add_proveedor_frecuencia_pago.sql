-- Corrección post-entrega: cada proveedor debe declarar cada cuánto se le
-- paga (obligatorio), separado de las condiciones comerciales (opcionales).
ALTER TABLE proveedor ADD COLUMN frecuencia_pago VARCHAR(60) NOT NULL DEFAULT '30 días';
ALTER TABLE proveedor ALTER COLUMN frecuencia_pago DROP DEFAULT;
