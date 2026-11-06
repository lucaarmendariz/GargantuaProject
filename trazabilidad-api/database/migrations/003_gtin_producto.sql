-- Añade a los productos su codigo de barras comercial (EAN/GTIN) sin perder datos.
-- Con el, al escanear la etiqueta GS1 de un lote ((01) GTIN) se reconoce el producto.
-- Se puede repetir sin riesgo. Despues hay que recargar 02_procedures.sql.
-- Las bases nuevas no la necesitan: 01_schema.sql ya incluye estos cambios.

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS gtin VARCHAR(14) DEFAULT NULL AFTER sku,
    ADD UNIQUE INDEX IF NOT EXISTS uq_product_gtin (gtin);
