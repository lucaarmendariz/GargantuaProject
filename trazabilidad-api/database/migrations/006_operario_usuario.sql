-- Guarda tambien el usuario de quien hace cada movimiento (historial de lotes y eventos de pedidos),
-- para mostrar su foto. Se puede repetir sin riesgo. Despues hay que recargar 02_procedures.sql.
-- Los movimientos anteriores quedan con NULL: su foto se busca por el nombre.
-- Las bases nuevas no la necesitan: 01_schema.sql ya incluye este cambio.

ALTER TABLE lot_history
    ADD COLUMN IF NOT EXISTS operator_username VARCHAR(50) DEFAULT NULL AFTER operator;

ALTER TABLE order_event
    ADD COLUMN IF NOT EXISTS operator_username VARCHAR(50) DEFAULT NULL AFTER operator;
