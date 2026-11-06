-- Añade los estados PREPARADO y ENTREGADO a una base de datos existente sin perder datos.
-- Se puede repetir sin riesgo. Despues hay que recargar 02_procedures.sql.
-- Las bases nuevas no la necesitan: 01_schema.sql ya incluye estos cambios.
--
-- Ciclo: ABIERTO -> PREPARADO -> ENTREGADO -> CERRADO (automatico tras la entrega).
-- Los pedidos que ya estaban CERRADO se quedan asi: pasan al apartado de pedidos cerrados.

ALTER TABLE customer_order
    ADD COLUMN IF NOT EXISTS prepared_by VARCHAR(100) DEFAULT NULL AFTER closed_by,
    ADD COLUMN IF NOT EXISTS delivered_by VARCHAR(100) DEFAULT NULL AFTER prepared_by,
    ADD COLUMN IF NOT EXISTS prepared_at TIMESTAMP NULL DEFAULT NULL AFTER closed_at,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP NULL DEFAULT NULL AFTER prepared_at,
    ADD INDEX IF NOT EXISTS idx_order_delivered (status, delivered_at);
