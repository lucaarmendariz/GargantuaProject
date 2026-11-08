-- Añade la foto de perfil de los usuarios a una base de datos existente sin perder datos.
-- Se puede repetir sin riesgo. Despues hay que recargar 02_procedures.sql.
-- Las bases nuevas no la necesitan: 01_schema.sql ya incluye este cambio.

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS avatar MEDIUMTEXT DEFAULT NULL AFTER name;
