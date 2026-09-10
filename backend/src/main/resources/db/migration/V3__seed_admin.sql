-- Módulo 0 · Fase 0.D — usuario ADMIN_GENERAL inicial.
-- Credenciales de arranque: admin@optiplant.local / Admin123!
-- password_hash: BCrypt (cost 10). Verificable con BCryptPasswordEncoder.
INSERT INTO usuario (nombre, email, password_hash, rol, sucursal_id)
VALUES ('Administrador General',
        'admin@optiplant.local',
        '$2a$10$655AnwyRPg43M5eo02KX/eaXpd4M/0CdbDW1RrLjQi90jRxfCl8fO',
        'ADMIN_GENERAL',
        NULL);
