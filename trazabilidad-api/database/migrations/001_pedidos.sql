-- Prepara una base de datos existente para los pedidos sin perder datos.
-- Se puede repetir sin riesgo. Despues hay que recargar 02_procedures.sql.
-- Las bases nuevas no la necesitan: 01_schema.sql ya incluye estos cambios.

ALTER TABLE product_lot
    ADD COLUMN IF NOT EXISTS reserved_quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000 AFTER quantity,
    ADD COLUMN IF NOT EXISTS consumed_quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000 AFTER reserved_quantity;

-- Pedidos: cada uno agrupa los lotes que se van a usar
CREATE TABLE IF NOT EXISTS customer_order (
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

ALTER TABLE lot_history
    ADD COLUMN IF NOT EXISTS order_id INT DEFAULT NULL,
    ADD CONSTRAINT fk_history_order FOREIGN KEY IF NOT EXISTS (order_id) REFERENCES customer_order (order_id);


-- Lineas del pedido: cantidad reservada de un lote y lo realmente consumido al cerrar
CREATE TABLE IF NOT EXISTS order_line (
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
CREATE TABLE IF NOT EXISTS order_event (
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
