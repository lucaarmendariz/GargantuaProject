#!/bin/bash

set -e

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "❌ Falta database/.env: copia .env.example a .env y rellena los valores."
  exit 1
fi
set -a
source ./.env
set +a

# La contraseña viaja por el entorno para que no aparezca en la lista de procesos
root_sql() { MYSQL_PWD="$DB_ROOT_PASSWORD" docker exec -i -e MYSQL_PWD trazabilidad_db mariadb -uroot "$@"; }
app_sql() { MYSQL_PWD="$DB_PASSWORD" docker exec -i -e MYSQL_PWD trazabilidad_db mariadb -u"$DB_USER" "$DB_NAME"; }

# Configuración única unificada
DB_USER="trazabilidad_app"
DB_NAME="traceability_lean"

echo "================================================="
echo "🧨 SCRIPT 2: RESET COMPLETO + LIMPIEZA DE DATOS"
echo "================================================="

# 1. Parar contenedor y BORRAR el volumen de datos (Evita duplicados)
echo "🧹 Eliminando contenedor y base de datos antigua..."
docker compose down -v

# 2. Levantar Docker desde cero (Limpio de fábrica)
echo "🐳 Creando contenedor MariaDB nuevo..."
docker compose up -d

# 3. Esperar a MariaDB (Entramos como root/root que viene por defecto)
echo "⏳ Esperando a que MariaDB inicie sus servicios..."
until root_sql -e "SELECT 1" >/dev/null 2>&1
do
  sleep 2
done

echo "✅ MariaDB lista para recibir scripts"

# 4. COMANDO MAESTRO (ROOT) - Para crear el entorno e inicializar
echo "🔐 [ROOT] Creando base de datos '$DB_NAME'..."
root_sql < 00_init_user_db.sql

# 5. COMANDO DE APLICACIÓN - Para rellenar las tablas sin duplicados
echo "🏗️ [APP] Ejecutando estructura del esquema (01_schema.sql)..."
app_sql < 01_schema.sql

echo "⚙️ [APP] Cargando procedimientos almacenados (02_procedures.sql)..."
app_sql < 02_procedures.sql

echo "🌱 [APP] Insertando semillas limpias (03_seeds.sql)..."
app_sql < 03_seeds.sql

echo "================================================="
echo "🚀 TODO LISTO: BASE DE DATOS TOTALMENTE RECREADA"
echo "================================================="