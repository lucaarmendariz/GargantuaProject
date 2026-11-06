#!/bin/bash
# Crea el certificado HTTPS del servidor de desarrollo para poder abrir la app
# desde un móvil de la misma WiFi (Safari en iPhone exige HTTPS para la cámara).
#
#   bash scripts/generar-certificado.sh                 # detecta las IP del PC
#   bash scripts/generar-certificado.sh 192.168.1.20    # o indícalas a mano
#
# Genera una autoridad de certificación propia (CA) y, firmado por ella, el
# certificado del servidor con localhost y las IP indicadas. La CA se instala
# una vez en el móvil como de confianza; si cambia la IP del PC basta con volver
# a ejecutar el script (la CA se reutiliza y no hay que reinstalarla).
#
# Resultado (no se sube al repositorio):
#   certs/gargantua-ca.key / .crt   autoridad de certificación
#   certs/servidor.key / .crt       certificado que usa Vite

set -euo pipefail
export MSYS_NO_PATHCONV=1 # Git Bash en Windows: no convertir "/CN=..." en rutas

cd "$(dirname "$0")/.."
mkdir -p certs

ips=("$@")
if [ ${#ips[@]} -eq 0 ]; then
  if command -v ipconfig >/dev/null 2>&1; then
    # Windows: IPv4 de los adaptadores, sin las redes virtuales de Docker/WSL
    mapfile -t ips < <(ipconfig | tr -d '\r' | grep -E 'IPv4' | grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' | grep -vE '^(172\.(1[6-9]|2[0-9]|3[01])\.|127\.)')
  else
    mapfile -t ips < <(hostname -I 2>/dev/null | tr ' ' '\n' | grep -E '^([0-9]{1,3}\.){3}[0-9]{1,3}$' | grep -vE '^(172\.(1[6-9]|2[0-9]|3[01])\.|127\.)')
  fi
fi

san="DNS:localhost,IP:127.0.0.1"
for ip in "${ips[@]}"; do
  san="$san,IP:$ip"
done

if [ ! -f certs/gargantua-ca.key ]; then
  echo "Creando la autoridad de certificación de Gargantua..."
  openssl req -x509 -new -newkey rsa:2048 -nodes -sha256 -days 3650 \
    -keyout certs/gargantua-ca.key -out certs/gargantua-ca.crt \
    -subj "/CN=Gargantua Desarrollo CA/O=Gargantua" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" >/dev/null 2>&1
fi

echo "Creando el certificado del servidor para: ${san//,/ }"
cat > certs/servidor.ext <<EOF
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=$san
EOF
openssl req -new -newkey rsa:2048 -nodes -sha256 \
  -keyout certs/servidor.key -out certs/servidor.csr \
  -subj "/CN=Gargantua desarrollo/O=Gargantua" >/dev/null 2>&1
# iOS no acepta certificados de servidor de más de 825 días
openssl x509 -req -in certs/servidor.csr -CA certs/gargantua-ca.crt -CAkey certs/gargantua-ca.key \
  -CAcreateserial -out certs/servidor.crt -days 365 -sha256 -extfile certs/servidor.ext >/dev/null 2>&1
rm -f certs/servidor.csr certs/servidor.ext


echo "Listo. Reinicia el front (docker compose restart front) y abre en el móvil:"
for ip in "${ips[@]}"; do
  echo "  https://$ip:5173   (certificado de la CA: https://$ip:5173/gargantua-ca.crt)"
done
