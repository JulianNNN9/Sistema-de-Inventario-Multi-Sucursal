-- Módulo 9 · Fase B — datos de prueba del dominio "ferretería" para QA manual
-- y demos, sin ningún paso adicional a `docker compose up` sobre un volumen
-- limpio (Sección 1, regla global 4). El ADMIN_GENERAL sembrado en
-- V3__seed_admin.sql se conserva tal cual (no se duplica un segundo admin
-- genérico): esta migración solo agrega los otros 8 usuarios y el resto de
-- datos temáticos, de modo que el sistema queda con exactamente 9 usuarios.
--
-- Contraseña de prueba uniforme para los 8 usuarios de esta migración:
-- Test123!  →  BCrypt (cost 10), generado y verificado con BCryptPasswordEncoder.
-- Ver docs/DATOS_DE_PRUEBA.md para el detalle de credenciales y del
-- déficit/superávit sembrado a propósito para el recomendador de rebalanceo.

-- ============================================================================
-- 1) Catálogo de productos (28 productos, 7 categorías de ferretería)
-- ============================================================================
CREATE TEMP TABLE tmp_producto (
    sku           VARCHAR(60)   NOT NULL,
    nombre        VARCHAR(160)  NOT NULL,
    unidad        VARCHAR(30)   NOT NULL,
    costo         NUMERIC(12,2) NOT NULL,
    cantidad_base NUMERIC(12,2) NOT NULL,
    minimo_base   NUMERIC(12,2) NOT NULL
);

INSERT INTO tmp_producto (sku, nombre, unidad, costo, cantidad_base, minimo_base) VALUES
    -- Herramienta manual (HER-)
    ('HER-0001', 'Martillo de uña 16oz',                 'unidad', 18000,  60, 15),
    ('HER-0002', 'Destornillador Phillips #2',            'unidad',  6000,  80, 20),
    ('HER-0003', 'Llave ajustable 10 pulgadas',           'unidad', 22000,  50, 12),
    ('HER-0004', 'Juego de llaves allen 9 piezas',        'unidad', 15000,  45, 10),
    -- Herramienta eléctrica (ELE-HTA-)
    ('ELE-HTA-0001', 'Taladro percutor 1/2 pulgada 750W', 'unidad', 185000, 20, 5),
    ('ELE-HTA-0002', 'Amoladora angular 4.5 pulgadas',    'unidad', 145000, 18, 5),
    ('ELE-HTA-0003', 'Sierra circular 7.25 pulgadas',     'unidad', 210000, 12, 4),
    ('ELE-HTA-0004', 'Rotomartillo SDS-Plus',             'unidad', 320000, 10, 3),
    -- Tornillería y anclaje (TOR-)
    ('TOR-0001', 'Tornillo autorroscante 1 pulgada (caja x100)', 'caja',   12000, 100, 25),
    ('TOR-0002', 'Anclaje de expansión 3/8 pulgada',              'unidad',   900, 300, 60),
    ('TOR-0003', 'Tuerca hexagonal 1/4 pulgada (caja x100)',      'caja',    9500,  90, 20),
    ('TOR-0004', 'Arandela plana 3/8 pulgada (caja x100)',        'caja',    8500,  90, 20),
    -- Pintura (PIN-)
    ('PIN-0001', 'Pintura látex blanco (galón)',          'galón',  68000,  40, 10),
    ('PIN-0002', 'Pintura esmalte negro (galón)',         'galón',  75000,  30, 8),
    ('PIN-0003', 'Anticorrosivo rojo óxido (galón)',      'galón',  82000,  25, 8),
    ('PIN-0004', 'Rodillo de pintura 9 pulgadas',         'unidad',  9500,  60, 15),
    -- Plomería (PLO-)
    ('PLO-0001', 'Tubo PVC 1/2 pulgada x 6 metros',       'unidad', 14000,  70, 15),
    ('PLO-0002', 'Codo PVC 90 grados 1/2 pulgada',        'unidad',   900, 200, 40),
    ('PLO-0003', 'Llave de paso 1/2 pulgada',             'unidad', 11000,  45, 10),
    ('PLO-0004', 'Cinta teflón 1/2 pulgada',              'unidad',  1200, 250, 50),
    -- Eléctrico (ELE-)
    ('ELE-0001', 'Cable THHN 12 AWG',                     'metro',   2200, 500, 100),
    ('ELE-0002', 'Interruptor sencillo',                  'unidad',  5500,  90, 20),
    ('ELE-0003', 'Toma doble con polo a tierra',          'unidad',  8200,  80, 20),
    ('ELE-0004', 'Bombillo LED 9W',                       'unidad',  6800, 120, 30),
    -- Seguridad industrial (SEG-)
    ('SEG-0001', 'Casco de seguridad',                    'unidad', 24000,  40, 10),
    ('SEG-0002', 'Guantes de carnaza',                    'par',     9500,  60, 15),
    ('SEG-0003', 'Gafas de seguridad',                    'unidad',  7000,  70, 15),
    ('SEG-0004', 'Chaleco reflectivo',                    'unidad', 16000,  35, 10);

INSERT INTO producto (sku, nombre, unidad_medida_base)
SELECT sku, nombre, unidad FROM tmp_producto;

-- ============================================================================
-- 2) Sucursales (4, ciudades distintas)
-- ============================================================================
INSERT INTO sucursal (nombre, ciudad) VALUES
    ('Ferretería Central',         'Bogotá'),
    ('Ferretería Norte',           'Medellín'),
    ('Ferretería Sur',             'Cali'),
    ('Ferretería Zona Industrial', 'Barranquilla');

-- ============================================================================
-- 3) Usuarios (8: gerente + operador por cada una de las 4 sucursales).
--    El 9.º usuario (ADMIN_GENERAL) es admin@optiplant.local de V3, sin cambios.
--    Password de los 8: Test123!  (hash BCrypt cost 10, ver docs/DATOS_DE_PRUEBA.md)
-- ============================================================================
INSERT INTO usuario (nombre, email, password_hash, rol, sucursal_id) VALUES
    ('Camila Restrepo', 'gerente.central@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'GERENTE_SUCURSAL',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central')),
    ('Andrés Gómez', 'operador.central@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'OPERADOR_INVENTARIO',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central')),
    ('Laura Zapata', 'gerente.norte@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'GERENTE_SUCURSAL',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte')),
    ('Santiago Uribe', 'operador.norte@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'OPERADOR_INVENTARIO',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte')),
    ('Valentina Ospina', 'gerente.sur@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'GERENTE_SUCURSAL',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur')),
    ('Juan Pablo Rojas', 'operador.sur@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'OPERADOR_INVENTARIO',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur')),
    ('Daniela Mejía', 'gerente.industrial@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'GERENTE_SUCURSAL',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial')),
    ('Felipe Cardona', 'operador.industrial@ferreteria.local',
     '$2a$10$It9hZ91TCBaFu9jfK7OZJOUC6zgDD09Hw.znIuLDaANIIuTIVwxya', 'OPERADOR_INVENTARIO',
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'));

-- ============================================================================
-- 4) Inventario por sucursal: grilla base (28 productos × 4 sucursales) con
--    un multiplicador propio por sucursal para que los niveles no sean
--    idénticos en toda la red. El déficit/superávit deliberado se aplica al
--    final del script (sección 8), pisando estos valores base.
-- ============================================================================
INSERT INTO inventario_sucursal (producto_id, sucursal_id, cantidad_actual, stock_minimo, costo_promedio_ponderado)
SELECT
    p.id,
    s.id,
    ROUND(tp.cantidad_base * CASE s.nombre
        WHEN 'Ferretería Central'         THEN 1.3
        WHEN 'Ferretería Norte'           THEN 1.0
        WHEN 'Ferretería Sur'             THEN 0.7
        WHEN 'Ferretería Zona Industrial' THEN 0.9
    END),
    tp.minimo_base,
    tp.costo
FROM tmp_producto tp
JOIN producto p ON p.sku = tp.sku
CROSS JOIN sucursal s;

-- ============================================================================
-- 5) Proveedores
-- ============================================================================
INSERT INTO proveedor (nombre, frecuencia_pago, condiciones) VALUES
    ('Ferretera Andina S.A.S.', '30 días', 'Descuento del 5% en compras superiores a $2.000.000'),
    ('Distribuidora Metálicas del Valle', 'Contado', NULL);

-- ============================================================================
-- 6) Órdenes de compra ya RECIBIDA (histórico + costo promedio ponderado real)
-- ============================================================================

-- Orden 1: Ferretera Andina → Ferretería Central, amoladoras angulares.
INSERT INTO orden_compra (proveedor_id, sucursal_id, fecha, estado, plazo_pago) VALUES
    ((SELECT id FROM proveedor WHERE nombre = 'Ferretera Andina S.A.S.'),
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
     now() - interval '20 days', 'RECIBIDA',
     (SELECT frecuencia_pago FROM proveedor WHERE nombre = 'Ferretera Andina S.A.S.'));

INSERT INTO orden_compra_detalle (orden_id, producto_id, cantidad, precio_unitario, descuento)
SELECT oc.id, (SELECT id FROM producto WHERE sku = 'ELE-HTA-0002'), 10, 140000, 0
FROM orden_compra oc
WHERE oc.proveedor_id = (SELECT id FROM proveedor WHERE nombre = 'Ferretera Andina S.A.S.')
  AND oc.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

UPDATE inventario_sucursal inv
SET costo_promedio_ponderado = ROUND(
        (inv.cantidad_actual * inv.costo_promedio_ponderado + 10 * 140000) / (inv.cantidad_actual + 10), 2),
    cantidad_actual = inv.cantidad_actual + 10
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0002')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'INGRESO', 'COMPRA', 10, now() - interval '20 days',
       (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0002')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- Orden 2: Ferretera Andina → Ferretería Norte, tornillería.
INSERT INTO orden_compra (proveedor_id, sucursal_id, fecha, estado, plazo_pago) VALUES
    ((SELECT id FROM proveedor WHERE nombre = 'Ferretera Andina S.A.S.'),
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
     now() - interval '14 days', 'RECIBIDA',
     (SELECT frecuencia_pago FROM proveedor WHERE nombre = 'Ferretera Andina S.A.S.'));

INSERT INTO orden_compra_detalle (orden_id, producto_id, cantidad, precio_unitario, descuento)
SELECT oc.id, (SELECT id FROM producto WHERE sku = 'TOR-0001'), 50, 11500, 0
FROM orden_compra oc
WHERE oc.proveedor_id = (SELECT id FROM proveedor WHERE nombre = 'Ferretera Andina S.A.S.')
  AND oc.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');

UPDATE inventario_sucursal inv
SET costo_promedio_ponderado = ROUND(
        (inv.cantidad_actual * inv.costo_promedio_ponderado + 50 * 11500) / (inv.cantidad_actual + 50), 2),
    cantidad_actual = inv.cantidad_actual + 50
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');

INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'INGRESO', 'COMPRA', 50, now() - interval '14 days',
       (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');

-- Orden 3: Distribuidora Metálicas del Valle → Ferretería Sur, tubería PVC.
INSERT INTO orden_compra (proveedor_id, sucursal_id, fecha, estado, plazo_pago) VALUES
    ((SELECT id FROM proveedor WHERE nombre = 'Distribuidora Metálicas del Valle'),
     (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
     now() - interval '9 days', 'RECIBIDA',
     (SELECT frecuencia_pago FROM proveedor WHERE nombre = 'Distribuidora Metálicas del Valle'));

INSERT INTO orden_compra_detalle (orden_id, producto_id, cantidad, precio_unitario, descuento)
SELECT oc.id, (SELECT id FROM producto WHERE sku = 'PLO-0001'), 30, 13500, 5
FROM orden_compra oc
WHERE oc.proveedor_id = (SELECT id FROM proveedor WHERE nombre = 'Distribuidora Metálicas del Valle')
  AND oc.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');

UPDATE inventario_sucursal inv
SET costo_promedio_ponderado = ROUND(
        (inv.cantidad_actual * inv.costo_promedio_ponderado + 30 * 13500) / (inv.cantidad_actual + 30), 2),
    cantidad_actual = inv.cantidad_actual + 30
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');

INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'INGRESO', 'COMPRA', 30, now() - interval '9 days',
       (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');

-- ============================================================================
-- 7) Ventas históricas (12, repartidas en los últimos ~4 meses). Las 6 más
--    recientes (últimos 20 días) quedan dentro de la ventana de rotación de
--    30 días del Dashboard, con cantidades distintas por producto para que
--    "mayor" y "menor" rotación muestren datos reales desde el arranque.
-- ============================================================================

-- V1 · Central · hace 2 días · Anclaje de expansión.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
     (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local'),
     now() - interval '2 days', 52000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'TOR-0002'), 40, 1300, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central') AND v.total = 52000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 40
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0002')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 40, now() - interval '2 days',
       (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0002')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- V2 · Norte · hace 5 días · Bombillo LED.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
     (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local'),
     now() - interval '5 days', 285000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'ELE-0004'), 30, 9500, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte') AND v.total = 285000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 30
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0004')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 30, now() - interval '5 days',
       (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0004')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');

-- V3 · Sur · hace 8 días · Cinta teflón.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
     (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local'),
     now() - interval '8 days', 59500);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'PLO-0004'), 35, 1700, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur') AND v.total = 59500;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 35
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0004')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 35, now() - interval '8 days',
       (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0004')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');

-- V4 · Zona Industrial · hace 12 días · Tornillo autorroscante (caja).
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'),
     (SELECT id FROM usuario WHERE email = 'operador.industrial@ferreteria.local'),
     now() - interval '12 days', 136000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'TOR-0001'), 8, 17000, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial') AND v.total = 136000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 8
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 8, now() - interval '12 days',
       (SELECT id FROM usuario WHERE email = 'operador.industrial@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');

-- V5 · Central · hace 16 días · Destornillador Phillips.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
     (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local'),
     now() - interval '16 days', 42500);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'HER-0002'), 5, 8500, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central') AND v.total = 42500;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 5
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'HER-0002')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 5, now() - interval '16 days',
       (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'HER-0002')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- V6 · Norte · hace 20 días · Rodillo de pintura.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
     (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local'),
     now() - interval '20 days', 27000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'PIN-0004'), 2, 13500, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte') AND v.total = 27000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 2
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0004')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 2, now() - interval '20 days',
       (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0004')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');

-- V7 · Sur · hace 35 días (mes anterior) · Chaleco reflectivo.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
     (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local'),
     now() - interval '35 days', 69000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'SEG-0004'), 3, 23000, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur') AND v.total = 69000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 3
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0004')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 3, now() - interval '35 days',
       (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0004')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');

-- V8 · Zona Industrial · hace 48 días (mes anterior) · Cable THHN.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'),
     (SELECT id FROM usuario WHERE email = 'operador.industrial@ferreteria.local'),
     now() - interval '48 days', 155000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'ELE-0001'), 50, 3100, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial') AND v.total = 155000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 50
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 50, now() - interval '48 days',
       (SELECT id FROM usuario WHERE email = 'operador.industrial@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');

-- V9 · Central · hace 65 días (hace 2 meses) · Pintura látex blanco.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
     (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local'),
     now() - interval '65 days', 380000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'PIN-0001'), 4, 95000, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central') AND v.total = 380000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 4
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 4, now() - interval '65 days',
       (SELECT id FROM usuario WHERE email = 'operador.central@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0001')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- V10 · Norte · hace 78 días (hace 2 meses) · Llave ajustable.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
     (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local'),
     now() - interval '78 days', 186000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'HER-0003'), 6, 31000, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte') AND v.total = 186000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 6
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'HER-0003')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 6, now() - interval '78 days',
       (SELECT id FROM usuario WHERE email = 'operador.norte@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'HER-0003')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');

-- V11 · Sur · hace 95 días (hace 3 meses) · Codo PVC.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
     (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local'),
     now() - interval '95 days', 78000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'PLO-0002'), 60, 1300, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur') AND v.total = 78000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 60
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0002')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 60, now() - interval '95 days',
       (SELECT id FROM usuario WHERE email = 'operador.sur@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0002')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');

-- V12 · Zona Industrial · hace 108 días (hace 3 meses) · Guantes de carnaza.
INSERT INTO venta (sucursal_id, usuario_id, fecha, total) VALUES
    ((SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'),
     (SELECT id FROM usuario WHERE email = 'operador.industrial@ferreteria.local'),
     now() - interval '108 days', 135000);
INSERT INTO venta_detalle (venta_id, producto_id, cantidad, precio_unitario, descuento)
SELECT v.id, (SELECT id FROM producto WHERE sku = 'SEG-0002'), 10, 13500, 0
FROM venta v WHERE v.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial') AND v.total = 135000;
UPDATE inventario_sucursal SET cantidad_actual = cantidad_actual - 10
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');
INSERT INTO movimiento_inventario (inventario_id, tipo, motivo, cantidad, fecha, responsable_id)
SELECT inv.id, 'RETIRO', 'VENTA', 10, now() - interval '108 days',
       (SELECT id FROM usuario WHERE email = 'operador.industrial@ferreteria.local')
FROM inventario_sucursal inv
WHERE inv.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002')
  AND inv.sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');

-- ============================================================================
-- 8) Déficit y superávit deliberados (rebalanceo, RF-31) + alertas de
--    reabastecimiento (RF-29). Se aplican al final para no depender de los
--    ajustes de compras/ventas anteriores. Umbral de superávit: 1.5× mínimo.
-- ============================================================================

-- Taladro percutor: déficit en Sur, superávit en Central.
UPDATE inventario_sucursal SET cantidad_actual = 1
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');
UPDATE inventario_sucursal SET cantidad_actual = 25
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- Pintura látex blanco: déficit en Zona Industrial, superávit en Norte. Se
-- baja también el nivel (generoso por diseño) de Central para que Norte
-- quede sin ambigüedad como el origen con más excedente disponible.
UPDATE inventario_sucursal SET cantidad_actual = 2
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial');
UPDATE inventario_sucursal SET cantidad_actual = 45
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');
UPDATE inventario_sucursal SET cantidad_actual = 20
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- Casco de seguridad: agotado (urgencia ALTA) en Norte, superávit en Sur.
-- Mismo ajuste en Central por la razón anterior.
UPDATE inventario_sucursal SET cantidad_actual = 0
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte');
UPDATE inventario_sucursal SET cantidad_actual = 40
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur');
UPDATE inventario_sucursal SET cantidad_actual = 12
WHERE producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0001')
  AND sucursal_id = (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central');

-- ============================================================================
-- 9) Transferencias históricas cubriendo los 8 estados de la Sección 3, cada
--    una con su historial de eventos en transferencia_evento. No generan
--    movimiento_inventario adicional (no afectan los niveles sembrados en las
--    secciones 4/8): su propósito es poblar bandejas, dashboard y logística,
--    no la trazabilidad de stock.
-- ============================================================================

-- 9.1 PENDIENTE — recién solicitada, sin aprobar.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            estado, urgencia)
VALUES ((SELECT id FROM producto WHERE sku = 'ELE-HTA-0003'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
        5, 'PENDIENTE', 'MEDIA');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '1 day', 'Solicitud creada'
FROM transferencia t
WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0003') AND t.cantidad_solicitada = 5;

-- 9.2 RECHAZADA — la sucursal origen no la aprueba.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            estado, urgencia)
VALUES ((SELECT id FROM producto WHERE sku = 'PIN-0002'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'),
        10, 'RECHAZADA', 'BAJA');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '6 days', 'Solicitud creada'
FROM transferencia t
WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0002') AND t.cantidad_solicitada = 10;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'RECHAZADA', now() - interval '5 days', 'Solicitud rechazada por la sucursal origen'
FROM transferencia t
WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'PIN-0002') AND t.cantidad_solicitada = 10;

-- 9.3 EN_TRANSITO — aprobada y despachada, aún no llega.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            cantidad_enviada, estado, urgencia, transportista, fecha_estimada_llegada)
VALUES ((SELECT id FROM producto WHERE sku = 'TOR-0003'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
        50, 50, 'EN_TRANSITO', 'MEDIA', 'Transportes Rápidos S.A.S.', now() + interval '2 days');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '4 days', 'Solicitud creada'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0003') AND t.cantidad_solicitada = 50;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '3 days',
       'Solicitud aprobada por la sucursal origen; disponible para despacho'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0003') AND t.cantidad_solicitada = 50;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'EN_TRANSITO', now() - interval '1 day', 'Despachada con Transportes Rápidos S.A.S.'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'TOR-0003') AND t.cantidad_solicitada = 50;

-- 9.4 COMPLETADA — ciclo completo, se recibió todo lo enviado.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            cantidad_enviada, cantidad_recibida, estado, urgencia, transportista,
                            fecha_estimada_llegada, fecha_real_llegada)
VALUES ((SELECT id FROM producto WHERE sku = 'ELE-0003'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'),
        20, 20, 20, 'COMPLETADA', 'BAJA', 'Coordinadora Mercantil',
        now() - interval '5 days', now() - interval '4 days');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '12 days', 'Solicitud creada'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0003') AND t.cantidad_solicitada = 20;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '10 days',
       'Solicitud aprobada por la sucursal origen; disponible para despacho'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0003') AND t.cantidad_solicitada = 20;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'EN_TRANSITO', now() - interval '9 days', 'Despachada con Coordinadora Mercantil'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0003') AND t.cantidad_solicitada = 20;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'COMPLETADA', now() - interval '4 days', 'Recepción completa'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-0003') AND t.cantidad_solicitada = 20;

-- 9.5 CON_FALTANTES — llegó menos de lo enviado.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            cantidad_enviada, cantidad_recibida, estado, urgencia, transportista,
                            fecha_estimada_llegada, fecha_real_llegada)
VALUES ((SELECT id FROM producto WHERE sku = 'HER-0004'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
        30, 30, 22, 'CON_FALTANTES', 'MEDIA', 'Envía',
        now() - interval '8 days', now() - interval '7 days');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '15 days', 'Solicitud creada'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'HER-0004') AND t.cantidad_solicitada = 30;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '13 days',
       'Solicitud aprobada por la sucursal origen; disponible para despacho'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'HER-0004') AND t.cantidad_solicitada = 30;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'EN_TRANSITO', now() - interval '12 days', 'Despachada con Envía'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'HER-0004') AND t.cantidad_solicitada = 30;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'CON_FALTANTES', now() - interval '7 days', 'Recepción parcial: faltan 8 unidades'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'HER-0004') AND t.cantidad_solicitada = 30;

-- 9.6 CERRADA_AJUSTE — el faltante se cierra asumiendo la pérdida.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            cantidad_enviada, cantidad_recibida, estado, urgencia, transportista,
                            fecha_estimada_llegada, fecha_real_llegada)
VALUES ((SELECT id FROM producto WHERE sku = 'SEG-0002'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Zona Industrial'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
        15, 15, 10, 'CERRADA_AJUSTE', 'BAJA', 'Servientrega',
        now() - interval '20 days', now() - interval '19 days');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '30 days', 'Solicitud creada'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002') AND t.cantidad_solicitada = 15;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '28 days',
       'Solicitud aprobada por la sucursal origen; disponible para despacho'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002') AND t.cantidad_solicitada = 15;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'EN_TRANSITO', now() - interval '27 days', 'Despachada con Servientrega'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002') AND t.cantidad_solicitada = 15;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'CON_FALTANTES', now() - interval '19 days', 'Recepción parcial: faltan 5 unidades'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002') AND t.cantidad_solicitada = 15;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'CERRADA_AJUSTE', now() - interval '18 days', 'Faltante cerrado por ajuste de inventario'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'SEG-0002') AND t.cantidad_solicitada = 15;

-- 9.7 CERRADA_RECLAMACION — el faltante se cierra con reclamación formal.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            cantidad_enviada, cantidad_recibida, estado, urgencia, transportista,
                            fecha_estimada_llegada, fecha_real_llegada)
VALUES ((SELECT id FROM producto WHERE sku = 'ELE-HTA-0004'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Central'),
        6, 6, 4, 'CERRADA_RECLAMACION', 'ALTA', 'Transportes Rápidos S.A.S.',
        now() - interval '25 days', now() - interval '24 days');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '35 days', 'Solicitud creada'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0004') AND t.cantidad_solicitada = 6;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '33 days',
       'Solicitud aprobada por la sucursal origen; disponible para despacho'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0004') AND t.cantidad_solicitada = 6;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'EN_TRANSITO', now() - interval '32 days', 'Despachada con Transportes Rápidos S.A.S.'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0004') AND t.cantidad_solicitada = 6;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'CON_FALTANTES', now() - interval '24 days', 'Recepción parcial: faltan 2 unidades'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0004') AND t.cantidad_solicitada = 6;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'CERRADA_RECLAMACION', now() - interval '23 days', 'Reclamación formal generada a la sucursal origen'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'ELE-HTA-0004') AND t.cantidad_solicitada = 6;

-- 9.8 REENVIO_SOLICITADO — el faltante se resuelve pidiendo un reenvío, que
--     queda registrado como una nueva transferencia PENDIENTE independiente
--     (mismo mecanismo que TransferenciaService.resolver, tratamiento REENVIO).
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            cantidad_enviada, cantidad_recibida, estado, urgencia, transportista,
                            fecha_estimada_llegada, fecha_real_llegada)
VALUES ((SELECT id FROM producto WHERE sku = 'PLO-0003'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
        40, 40, 28, 'REENVIO_SOLICITADO', 'MEDIA', 'Coordinadora Mercantil',
        now() - interval '10 days', now() - interval '9 days');
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '18 days', 'Solicitud creada'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003') AND t.cantidad_solicitada = 40;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'PENDIENTE', now() - interval '16 days',
       'Solicitud aprobada por la sucursal origen; disponible para despacho'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003') AND t.cantidad_solicitada = 40;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'EN_TRANSITO', now() - interval '15 days', 'Despachada con Coordinadora Mercantil'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003') AND t.cantidad_solicitada = 40;
INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t.id, 'CON_FALTANTES', now() - interval '9 days', 'Recepción parcial: faltan 12 unidades'
FROM transferencia t WHERE t.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003') AND t.cantidad_solicitada = 40;

-- Nueva transferencia PENDIENTE por el faltante (12 unidades), mismo producto y ruta.
INSERT INTO transferencia (producto_id, sucursal_origen_id, sucursal_destino_id, cantidad_solicitada,
                            estado, urgencia)
VALUES ((SELECT id FROM producto WHERE sku = 'PLO-0003'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Norte'),
        (SELECT id FROM sucursal WHERE nombre = 'Ferretería Sur'),
        12, 'PENDIENTE', 'MEDIA');

INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t_original.id, 'REENVIO_SOLICITADO', now() - interval '8 days',
       'Reenvío solicitado: nueva transferencia #' ||
       (SELECT t_nueva.id FROM transferencia t_nueva
        WHERE t_nueva.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003')
          AND t_nueva.cantidad_solicitada = 12 AND t_nueva.estado = 'PENDIENTE')
FROM transferencia t_original
WHERE t_original.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003') AND t_original.cantidad_solicitada = 40;

INSERT INTO transferencia_evento (transferencia_id, estado, fecha, comentario)
SELECT t_nueva.id, 'PENDIENTE', now() - interval '8 days',
       'Reenvío por faltante de la transferencia #' ||
       (SELECT t_original.id FROM transferencia t_original
        WHERE t_original.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003')
          AND t_original.cantidad_solicitada = 40)
FROM transferencia t_nueva
WHERE t_nueva.producto_id = (SELECT id FROM producto WHERE sku = 'PLO-0003')
  AND t_nueva.cantidad_solicitada = 12 AND t_nueva.estado = 'PENDIENTE';

-- ============================================================================
-- 10) Lista de precios (1, global, con 6 productos — precio mayorista)
-- ============================================================================
INSERT INTO lista_precio (nombre, sucursal_id) VALUES ('Lista Ferretero Mayorista', NULL);

INSERT INTO lista_precio_detalle (lista_id, producto_id, precio)
SELECT (SELECT id FROM lista_precio WHERE nombre = 'Lista Ferretero Mayorista'), p.id, precio
FROM (VALUES
    ('HER-0001', 22000),
    ('ELE-HTA-0001', 225000),
    ('TOR-0001', 15000),
    ('PIN-0001', 85000),
    ('PLO-0001', 17500),
    ('SEG-0001', 30000)
) AS precios(sku, precio)
JOIN producto p ON p.sku = precios.sku;

DROP TABLE tmp_producto;
