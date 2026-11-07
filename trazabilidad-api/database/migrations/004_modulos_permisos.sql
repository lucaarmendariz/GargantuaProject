-- Añade los modulos contratados, los permisos por rol y el usuario SUPERADMIN a una base existente.
-- Se puede repetir sin riesgo. Despues hay que recargar 02_procedures.sql.
-- Las bases nuevas no la necesitan: 01_schema.sql y 03_seeds.sql ya incluyen estos cambios.

CREATE TABLE IF NOT EXISTS app_module (
          module_code VARCHAR(30) PRIMARY KEY,
          name VARCHAR(100) NOT NULL,
          sort_order INT NOT NULL DEFAULT 0,
          is_enabled TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permission (
          role VARCHAR(50) NOT NULL,
          module_code VARCHAR(30) NOT NULL,
          access VARCHAR(20) NOT NULL DEFAULT 'NINGUNO',
          PRIMARY KEY (role, module_code),
          CONSTRAINT fk_permission_module FOREIGN KEY (module_code) REFERENCES app_module (module_code)
) ENGINE=InnoDB;

INSERT IGNORE INTO app_module (module_code, name, sort_order) VALUES
       ('PEDIDOS', 'Pedidos', 1),
       ('INVENTARIO', 'Inventario', 2),
       ('LOTES', 'Gestión de lotes', 3),
       ('HISTORIAL', 'Historial de auditoría', 4),
       ('PRODUCCION', 'Producción en planta', 5);

-- Los mismos permisos que tenia cada rol antes de esta migracion
INSERT IGNORE INTO role_permission (role, module_code, access) VALUES
       ('ENCARGADO', 'PEDIDOS', 'GESTIONAR'), ('ENCARGADO', 'INVENTARIO', 'GESTIONAR'), ('ENCARGADO', 'LOTES', 'GESTIONAR'),
       ('ENCARGADO', 'HISTORIAL', 'NINGUNO'), ('ENCARGADO', 'PRODUCCION', 'NINGUNO'),
       ('OPERARIO', 'PEDIDOS', 'VER'), ('OPERARIO', 'INVENTARIO', 'NINGUNO'), ('OPERARIO', 'LOTES', 'NINGUNO'),
       ('OPERARIO', 'HISTORIAL', 'NINGUNO'), ('OPERARIO', 'PRODUCCION', 'VER');

-- Cuenta del proveedor de la app. CAMBIA SU CONTRASEÑA en una instalacion real:
-- este hash es el de la contraseña de prueba 'moncorp2026'.
INSERT IGNORE INTO app_user (username, password_hash, name, role, is_active)
VALUES ('superadmin', '$2a$10$A8ZBNb2G/0/Y4Lv1Ygw8iejFWQ.M5A3UPrxl5I2clCaCyyvhSL8s6', 'Gargantua (proveedor)', 'SUPERADMIN', 1);
