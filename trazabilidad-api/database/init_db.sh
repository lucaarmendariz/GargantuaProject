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

echo "================================================="
echo "🐳 SCRIPT 1: LEVANTANDO ENTORNO DOCKER MARIADB"
echo "================================================="

# 1. Levantar Docker (Si ya estaba levantado, no hace nada)
echo "🚀 Levantando contenedor..."
docker compose up -d

# 2. Esperar a que MariaDB esté lista (Usa las credenciales maestras del docker-compose)
echo "⏳ Esperando a que el motor de MariaDB responda..."
until root_sql -e "SELECT 1" >/dev/null 2>&1
do
  sleep 2
done

echo "================================================="
echo "✅ DOCKER LISTO - BASE DE DATOS EN MARCHA"
echo "================================================="