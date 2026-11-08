#!/usr/bin/env bash
# Instala la app en el servidor de un cliente. Se puede repetir (para actualizar): no borra nada.
#   1. Crea produccion/.env con contraseñas aleatorias (solo la primera vez)
#   2. Construye y arranca la base de datos, la API y la web
#   3. Si la base de datos no tiene usuarios, crea tu cuenta de proveedor y el administrador del cliente
#   4. Programa la copia de seguridad diaria
# Necesita Docker con el plugin compose y bash.
set -euo pipefail
cd "$(dirname "$0")"

secreto() { openssl rand -hex 32 2>/dev/null || od -An -tx1 -N32 /dev/urandom | tr -d ' \n'; }
# Sin mas entrada (p. ej. un fichero de respuestas incompleto) se para, en vez de preguntar sin fin
leer() { read -r "$@" || { echo "Faltan datos: instalacion interrumpida." >&2; exit 1; }; }

# --- 1. Configuracion de este cliente ---
if [ ! -f .env ]; then
  leer -p "Direccion de la app (dominio, IP de la planta, o :80 para probar): " dominio
  sed -e "s|^DOMINIO=.*|DOMINIO=$dominio|" \
      -e "s|^DB_ROOT_PASSWORD=.*|DB_ROOT_PASSWORD=$(secreto)|" \
      -e "s|^DB_PASSWORD=.*|DB_PASSWORD=$(secreto)|" \
      -e "s|^JWT_SECRET=.*|JWT_SECRET=$(secreto)|" .env.example > .env
  chmod 600 .env
  echo "Creado produccion/.env (guarda una copia: sin el no se puede abrir la base de datos)."
fi
set -a; source ./.env; set +a

# --- 2. Arrancar ---
docker compose up -d --build </dev/null
echo "Esperando a la base de datos..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' "$(docker compose ps -q db)")" = healthy ]; do sleep 2; done

sql() { docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" db mariadb -utrazabilidad_app traceability_lean "$@"; }

# --- 3. Usuarios iniciales (solo con la base de datos vacia) ---
# Hash BCrypt con htpasswd (imagen oficial de Apache): no hace falta instalar nada en el servidor
hash() { printf '%s' "$1" | docker run --rm -i httpd:2.4-alpine htpasswd -niBC 10 "" | tr -d ':\r\n' | sed 's/^\$2y/$2a/'; }

pedir_clave() {
  local clave repetida
  while true; do
    leer -sp "  Contraseña (8 a 72 caracteres): " clave; echo >&2
    leer -sp "  Repítela: " repetida; echo >&2
    if [ "$clave" != "$repetida" ]; then echo "  No coinciden." >&2
    elif [ ${#clave} -lt 8 ] || [ ${#clave} -gt 72 ]; then echo "  Debe tener entre 8 y 72 caracteres." >&2
    else printf '%s' "$clave"; return; fi
  done
}

# </dev/null: docker compose exec se quedaria con lo que venga por la entrada (las respuestas siguientes)
if [ "$(sql -N -e 'SELECT COUNT(*) FROM app_user' </dev/null)" = 0 ]; then
  echo "Tu cuenta de proveedor (usuario superadmin, para activar los modulos contratados):" >&2
  clave_super=$(pedir_clave)

  echo "Administrador del cliente:" >&2
  until [[ "${admin_usuario:-}" =~ ^[A-Za-z0-9._-]{3,50}$ ]]; do
    leer -p "  Usuario para entrar (3 a 50 letras, numeros, . _ -): " admin_usuario
  done
  until [ -n "${admin_nombre:-}" ] && [ ${#admin_nombre} -le 100 ]; do
    leer -p "  Nombre y apellidos: " admin_nombre
  done
  clave_admin=$(pedir_clave)
  # Comillas del nombre escapadas para SQL (el usuario ya solo admite letras, numeros y . _ -)
  nombre_sql=${admin_nombre//\'/\'\'}

  sql <<SQL
INSERT INTO app_user (username, password_hash, name, role) VALUES
  ('superadmin', '$(hash "$clave_super")', 'Gargantua (proveedor)', 'SUPERADMIN'),
  ('$admin_usuario', '$(hash "$clave_admin")', '$nombre_sql', 'ADMIN');
SQL
  echo "Usuarios creados: superadmin y $admin_usuario."
else
  echo "La base de datos ya tiene usuarios: no se crea ninguno."
fi

# --- 4. Copia de seguridad diaria a las 03:00 ---
mkdir -p copias
if command -v crontab >/dev/null 2>&1; then
  (crontab -l 2>/dev/null | grep -vF "$PWD/backup.sh" || true; echo "0 3 * * * $PWD/backup.sh >> $PWD/copias/backup.log 2>&1") | crontab -
  echo "Copia de seguridad programada a diario a las 03:00 (copias en produccion/copias)."
else
  echo "AVISO: no hay crontab en este equipo. Programa ./backup.sh a diario a mano."
fi

case "$DOMINIO" in
  :*) echo "Listo: http://localhost:${PUERTO_HTTP:-80}" ;;
  *)  echo "Listo: https://$DOMINIO" ;;
esac
