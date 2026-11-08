#!/usr/bin/env bash
# Copia de seguridad de la base de datos del cliente. instalar.sh la programa a diario a las 03:00.
# Guarda produccion/copias/gargantapp-AAAAMMDD-HHMM.sql.gz, borra las de mas de DIAS_COPIAS dias y,
# si DESTINO_REMOTO esta en .env, la copia fuera del servidor con rclone.
# Restaurar: ver el README ("Copias de seguridad").
set -euo pipefail
cd "$(dirname "$0")"
set -a; source ./.env; set +a

mkdir -p copias
fichero="copias/gargantapp-$(date +%Y%m%d-%H%M).sql.gz"
# Si algo falla no queda una copia a medias que parezca buena
trap 'rm -f "$fichero.tmp"' EXIT

# --single-transaction: copia coherente sin parar la app; --routines: tambien los procedimientos
docker compose exec -T -e MYSQL_PWD="$DB_ROOT_PASSWORD" db \
  mariadb-dump -uroot --single-transaction --routines traceability_lean | gzip > "$fichero.tmp"
mv "$fichero.tmp" "$fichero"
echo "$(date '+%F %T') Copia creada: $fichero ($(du -h "$fichero" | cut -f1))"

find copias -name 'gargantapp-*.sql.gz' -mtime +"${DIAS_COPIAS:-30}" -delete

if [ -n "${DESTINO_REMOTO:-}" ]; then
  rclone copy "$fichero" "$DESTINO_REMOTO"
  echo "$(date '+%F %T') Copiada a $DESTINO_REMOTO"
fi
