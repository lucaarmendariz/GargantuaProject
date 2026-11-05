-- =================================================================
-- 1. LIMPIEZA DE TABLAS (Para que al ejecutar el update_db.sh se limpie la base de datos por completo)
-- =================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS order_event;
DROP TABLE IF EXISTS order_line;
DROP TABLE IF EXISTS lot_history;
DROP TABLE IF EXISTS customer_order;
DROP TABLE IF EXISTS product_lot;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS app_user;
SET FOREIGN_KEY_CHECKS = 1;

-- =================================================================
-- 2. CREACIÓN DE TABLAS MAESTRAS
-- =================================================================

-- Tabla de Productos
CREATE TABLE product (
         product_id INT AUTO_INCREMENT PRIMARY KEY,
         sku VARCHAR(50) UNIQUE NOT NULL,
         name VARCHAR(100) NOT NULL,
         description TEXT DEFAULT NULL,
         is_active TINYINT(1) DEFAULT 1 NOT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Tabla de Lotes
CREATE TABLE product_lot (
         lot_id INT AUTO_INCREMENT PRIMARY KEY,
         product_id INT NOT NULL,
         lot_number VARCHAR(100) UNIQUE NOT NULL,
         quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000, -- Cantidad de entrada
         reserved_quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000, -- Comprometida en pedidos abiertos
         consumed_quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000, -- Gastada en pedidos cerrados
         measure_unit VARCHAR(20) NOT NULL DEFAULT 'UNITS',
         expiration_date bigint(20) DEFAULT NULL,
         supplier_reference VARCHAR(100) DEFAULT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
         CONSTRAINT fk_lot_product FOREIGN KEY (product_id) REFERENCES product (product_id)
) ENGINE=InnoDB;

-- Pedidos: cada uno agrupa los lotes que se van a usar
CREATE TABLE customer_order (
         order_id INT AUTO_INCREMENT PRIMARY KEY,
         order_number VARCHAR(100) UNIQUE NOT NULL,
         customer_name VARCHAR(150) DEFAULT NULL,
         scheduled_date BIGINT NOT NULL, -- Dia programado (epoch en segundos)
         status VARCHAR(20) NOT NULL DEFAULT 'ABIERTO', -- ABIERTO, CERRADO, CANCELADO
         notes TEXT DEFAULT NULL,
         created_by VARCHAR(100) NOT NULL,
         closed_by VARCHAR(100) DEFAULT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         closed_at TIMESTAMP NULL DEFAULT NULL,
         INDEX idx_order_schedule (scheduled_date, status)
) ENGINE=InnoDB;

-- Tabla de Historial (Log de movimientos y captura OCR)
CREATE TABLE lot_history (
         history_id INT AUTO_INCREMENT PRIMARY KEY,
         lot_id INT NOT NULL,
         action_type VARCHAR(50) NOT NULL,
         operator VARCHAR(100) NOT NULL,
         action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
         quantity_affected DECIMAL(12,4) DEFAULT NULL,
         ocr_raw_text TEXT DEFAULT NULL,
         observations TEXT DEFAULT NULL,
         order_id INT DEFAULT NULL, -- Pedido que origino el movimiento
         CONSTRAINT fk_history_lot FOREIGN KEY (lot_id) REFERENCES product_lot (lot_id) ON DELETE CASCADE ON UPDATE CASCADE,
         CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES customer_order (order_id)
) ENGINE=InnoDB;

-- Tabla de Usuarios del Sistema
CREATE TABLE app_user (
          user_id INT AUTO_INCREMENT PRIMARY KEY,
          username VARCHAR(50) UNIQUE NOT NULL,
          password_hash VARCHAR(255) NOT NULL, -- Guardaremos las contraseñas cifradas
          name VARCHAR(100) NOT NULL,
          role VARCHAR(50) NOT NULL DEFAULT 'OPERARIO', -- OPERARIO, ENCARGADO, ADMIN
          is_active TINYINT(1) DEFAULT 1 NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- Lineas del pedido: cantidad reservada de un lote y lo realmente consumido al cerrar
CREATE TABLE order_line (
         line_id INT AUTO_INCREMENT PRIMARY KEY,
         order_id INT NOT NULL,
         lot_id INT NOT NULL,
         reserved_quantity DECIMAL(12,4) NOT NULL,
         consumed_quantity DECIMAL(12,4) DEFAULT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         UNIQUE KEY uq_order_lot (order_id, lot_id),
         CONSTRAINT fk_line_order FOREIGN KEY (order_id) REFERENCES customer_order (order_id),
         CONSTRAINT fk_line_lot FOREIGN KEY (lot_id) REFERENCES product_lot (lot_id)
) ENGINE=InnoDB;

-- Historial completo de cada pedido (no se modifica ni se borra)
CREATE TABLE order_event (
         event_id INT AUTO_INCREMENT PRIMARY KEY,
         order_id INT NOT NULL,
         lot_id INT DEFAULT NULL,
         event_type VARCHAR(30) NOT NULL, -- CREADO, RESERVADO, LIBERADO, CONSUMIDO, CERRADO, CANCELADO
         quantity DECIMAL(12,4) DEFAULT NULL,
         operator VARCHAR(100) NOT NULL,
         notes TEXT DEFAULT NULL,
         event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
         CONSTRAINT fk_event_order FOREIGN KEY (order_id) REFERENCES customer_order (order_id),
         CONSTRAINT fk_event_lot FOREIGN KEY (lot_id) REFERENCES product_lot (lot_id)
) ENGINE=InnoDB;
