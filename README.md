# GargantuaProject

> ⚠️ COMPLETAR: una frase que resuma el proyecto. Ejemplo: *Aplicación web para el seguimiento y la trazabilidad de [productos / lotes / equipos / pedidos] a lo largo de su ciclo de vida.*

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-orange)

# Gargantua

[Descripción breve del proyecto: qué hace la aplicación y para quién.]

Este repositorio contiene el backend y el frontend en el mismo lugar, pero **se desarrollan por separado**: cada parte tiene su carpeta, su entorno de desarrollo y su forma de arrancar.

## Estructura del repositorio

```
GargantuaProject/
├── trazabilidad-api/               Backend (API REST)
│   └── database/                   Scripts SQL y update_db.sh
└── GargantAppFront/
    └── gargantapp-front/           Frontend (Vue)
```

| Parte | Carpeta | Tecnología | IDE |
|-------|---------|------------|-----|
| Backend | `trazabilidad-api` | Java 17, Spring Boot 3.3.5, Gradle, MariaDB (JDBC) | IntelliJ IDEA |
| Frontend | `GargantAppFront/gargantapp-front` | Vue | Visual Studio Code |

## Requisitos

- **Git**
- **JDK 17**
- **Node.js LTS** (incluye npm)
- **MariaDB** (instalada en local o con Docker)
- **IntelliJ IDEA** para el backend y **Visual Studio Code** para el frontend
- En Windows, **Git Bash** para ejecutar el script `update_db.sh`

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/lucarmendariz/GargantuaProject.git
cd GargantuaProject
```

### 2. Secretos y base de datos

Copia la plantilla y sustituye cada valor (puedes generarlos con `openssl rand -hex 32`):

```bash
cd trazabilidad-api/database
cp .env.example .env
```

`.env` no se sube al repositorio. Después crea la base de datos con sus tablas y datos de prueba:

```bash
bash update_db.sh
```

> `update_db.sh` **borra la base de datos** y la crea de nuevo. Para arrancarla sin perder datos usa `bash init_db.sh`.
>
> En Windows, ejecuta los scripts desde **Git Bash**. Si dan errores de `\r`, convierte el archivo a finales de línea LF.

Usuario de prueba: `luca` / `moncorp2026`. Es público, así que no lo uses fuera de desarrollo.

**Datos de demo (opcional).** `04_demo_data.sql` rellena todas las pantallas: un usuario de cada rol (`ane` ENCARGADO y `mikel` OPERARIO, misma contraseña), productos de la planta de tortillas, lotes vigentes, a punto de caducar, caducados y bloqueados, historial de planta y pedidos abiertos, cerrados y cancelados con fechas relativas a hoy. Se puede repetir sin duplicar nada. Desde `trazabilidad-api/database`:

```bash
set -a; source ./.env; set +a
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < 04_demo_data.sql
```

### Datos de prueba automáticos en desarrollo (perfil `dev`)

Con el perfil `dev` la API **reinicia la base de datos con los datos de prueba cada vez que arranca**: borra todas las tablas y ejecuta `01_schema.sql`, `02_procedures.sql`, `03_seeds.sql` y `04_demo_data.sql`. Además vigila esos cuatro scripts y, **si guardas un cambio en alguno, repite el reinicio** sin parar la API (si el script tiene un error, se ve en el log y la API sigue en marcha). Así siempre se trabaja con los mismos datos y con el esquema al día, sin aplicar migraciones a mano.

- `docker compose up -d` y `./gradlew bootRun` ya usan el perfil `dev`. Desde IntelliJ, añade `SPRING_PROFILES_ACTIVE=dev` en las variables de entorno de la configuración de ejecución y pon como *Working directory* la carpeta `trazabilidad-api`.
- Para conservar los datos entre arranques: `DEV_RESET_DB=false docker compose up -d`, o `app.dev.reset-db=false` en `application-dev.properties`.
- Por seguridad solo reinicia bases de datos locales (`localhost`, `127.0.0.1` o `host.docker.internal`). **No uses el perfil `dev` en producción.**

### Todo con Docker (sin instalar JDK ni Node)

Con la base de datos ya en marcha, desde la raíz del repositorio `docker compose up -d` arranca la API (`http://localhost:8080`) y el frontend (`https://localhost:5173`, con un certificado local: el navegador avisa la primera vez). El frontend llama a la API a través de `/api`, que Vite reenvía al contenedor de la API. Para usar HTTP sin certificado: `HTTPS=false docker compose up -d`.

### Escanear etiquetas con la cámara del móvil

En *Inventario → Añadir lote*, en la *Cámara OCR* del Panel Central y en *Producción Planta* hay un botón **Escanear con la cámara**. Lee códigos de barras y QR (Code128, EAN, QR, DataMatrix y GS1 con lote `(10)`, caducidad `(17)` y cantidad `(37)`) y, si la etiqueta no tiene código, reconoce el texto por OCR (`LOTE: ...`, `CAD: dd/mm/aaaa`, `ALB-...`). Los campos se rellenan solos y se revisan antes de guardar. El producto se reconoce por su código de barras (EAN/GTIN, campo del producto en *Inventario*) o por el SKU escrito en la etiqueta; si el código aún no es de ningún producto, al dar de alta el lote se guarda en el producto elegido y la próxima vez se reconoce solo.

Para usarlo desde un móvil (Safari solo deja usar la cámara en HTTPS, y el iPhone no acepta el certificado provisional de Vite):

1. En el PC, desde `GargantAppFront/gargantapp-front`, crea el certificado con la IP del PC y reinicia el front:

   ```bash
   bash scripts/generar-certificado.sh
   docker compose restart front
   ```

   Crea una autoridad de certificación propia (CA) y un certificado para `localhost` y las IP del PC, en `certs/` (no se sube al repositorio). Si cambia la IP, vuelve a ejecutarlo: la CA se mantiene.
2. El móvil y el PC tienen que estar en la misma WiFi. En el iPhone, abre en Safari `https://<IP del PC>:5173/gargantua-ca.crt`, acepta el aviso (*Mostrar detalles → visitar este sitio web*) y permite descargar el perfil.
3. Instálalo en *Ajustes → General → VPN y gestión de dispositivos → Gargantua Desarrollo CA → Instalar*.
4. Actívalo en *Ajustes → General → Información → Ajustes de confianza de certificados → Gargantua Desarrollo CA*.
5. Abre `https://<IP del PC>:5173`: ya no sale ningún aviso. La primera vez que escanees, permite el acceso a la cámara.

El OCR descarga el idioma la primera vez que se usa, así que el móvil necesita internet en ese momento.

### 3. Backend

1. Abre la carpeta `trazabilidad-api` en **IntelliJ IDEA** (no la raíz del repositorio).
2. Activa el procesamiento de anotaciones para Lombok: *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing*.
3. Arranca la aplicación desde IntelliJ o por terminal. La configuración se lee de `database/.env`, con `trazabilidad-api` como directorio de trabajo:

   ```bash
   cd trazabilidad-api
   ./gradlew bootRun
   ```

   En Windows: `gradlew.bat bootRun`. La API queda disponible en `http://localhost:8080`.

La API exige un token JWT, que se obtiene en `POST /api/auth/login` y caduca a las 8 horas:

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Todo |
| `ENCARGADO` | Productos, lotes y estado de lotes |
| `OPERARIO` | Registrar movimientos de producción y consultar el estado de lotes |

### 4. Frontend

1. Abre la carpeta `GargantAppFront/gargantapp-front` en **Visual Studio Code**.
2. Instala las dependencias y arranca el servidor de desarrollo:

   ```bash
   cd GargantAppFront/gargantapp-front
   npm install
   npm run dev
   ```

   [Confirma el comando en la sección `scripts` de `package.json`: puede ser `npm run dev` o `npm run serve`.]

3. Abre la dirección que muestre la terminal (normalmente `http://localhost:5173`).

> El frontend necesita que el backend esté en marcha. Si el navegador muestra errores de CORS, el backend debe permitir el origen del frontend.

## Pedidos y stock

Un pedido tiene una fecha programada y una línea por cada lote que usa. Pasa por estos estados:

`ABIERTO` → `PREPARADO` → `ENTREGADO` → `CERRADO` (automático). Desde `ABIERTO` también se puede `CANCELAR`.

1. **Reservar** (abierto): al asignar un lote baja su cantidad *disponible*, pero no su stock.
2. **Preparar:** se indica lo realmente consumido en cada línea. Eso se resta del stock y se libera lo reservado. O se registran todas las líneas o no cambia nada. Desde aquí los lotes del pedido ya no se pueden cambiar.
3. **Entregar:** se marca cuando el pedido sale hacia el cliente.
4. **Cerrar (automático):** a las 48 h de la entrega la API pasa el pedido a `CERRADO` (`app.orders.auto-close-hours`). Los cerrados se consultan en la pestaña *Pedidos cerrados* del módulo Pedidos, con buscador.
5. **Cancelar o retirar un lote** (solo abierto): se libera lo reservado sin tocar el stock.

`stock = entrada - consumido` y `disponible = stock - reservado`. Un lote cuyo último movimiento manual es `BLOQUEADO` no se puede reservar ni consumir. Los lotes caducados se señalan, pero no se bloquean.

| Rol | Pedidos |
|-----|---------|
| `ADMIN`, `ENCARGADO` | Crear, asignar y retirar lotes, preparar, entregar y cancelar |
| `OPERARIO` | Consultar, preparar y entregar |

**Actualizar una base de datos existente sin perder datos**, desde `trazabilidad-api/database`:

```bash
set -a; source ./.env; set +a
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/001_pedidos.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/002_estados_pedido.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/003_gtin_producto.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < 02_procedures.sql
```

## Flujo de trabajo

- Trabaja cada parte en su propio IDE y abre solo su carpeta.
- Haz commits que afecten, siempre que sea posible, a una sola parte (backend o frontend) para que el historial sea fácil de leer.
- No subas contraseñas ni claves. Los valores reales de `application.properties` y los archivos `.env` deben quedar fuera del repositorio.

## Problemas frecuentes

- **Puerto ocupado:** cambia `server.port` en el backend o el puerto del servidor de desarrollo del frontend.
- **Error de conexión a la base de datos o `Could not resolve placeholder`:** comprueba que MariaDB está arrancada, que existe `database/.env` y que el directorio de trabajo del backend es `trazabilidad-api`.
- **Versión de Java incorrecta:** comprueba con `java -version` que tienes la 17.
- **`mariadb: command not found` en `update_db.sh`:** instala el cliente de MariaDB o ejecuta los `.sql` dentro del contenedor de Docker.

## Índice

- [Descripción](#descripción)
- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Requisitos previos](#requisitos-previos)
- [Instalación y puesta en marcha](#instalación-y-puesta-en-marcha)
- [Variables de entorno](#variables-de-entorno)
- [Documentación de la API](#documentación-de-la-api)
- [Tests](#tests)
- [Hoja de ruta](#hoja-de-ruta)
- [Autoría](#autoría)

## Descripción

⚠️ COMPLETAR: explica en 3-5 líneas **qué problema resuelve**, **quién lo usa** y **qué se traza** (por ejemplo, qué entidades se siguen y qué eventos se registran).

El proyecto se divide en dos partes independientes:

- **`trazabilidad-api`**: API que gestiona los datos y la lógica de negocio.
- **`GargantAppFront/gargantapp-front`**: aplicación web que consume la API.

## Funcionalidades

⚠️ COMPLETAR: deja solo lo que **ya está implementado** y mueve el resto a la hoja de ruta.

- [ ] Autenticación y gestión de usuarios
- [ ] Roles y permisos
- [ ] Registro de [entidades principales]
- [ ] Historial de movimientos / eventos de cada elemento
- [ ] Búsqueda y filtrado
- [ ] Informes o exportación de datos

## Tecnologías

| Capa | Tecnología |
|------|------------|
| API | ⚠️ COMPLETAR (lenguaje y framework) |
| Base de datos | ⚠️ COMPLETAR |
| Frontend | ⚠️ COMPLETAR (framework, versión) |
| Autenticación | ⚠️ COMPLETAR (token, sesión, API Key…) |
| Otros | ⚠️ COMPLETAR (Docker, tests, linter…) |

## Estructura del repositorio

```
GargantuaProject/
├── GargantAppFront/
│   └── gargantapp-front/   # Frontend
└── trazabilidad-api/       # Backend / API
```

## Requisitos previos

⚠️ COMPLETAR con versiones concretas, por ejemplo:

- Node.js ≥ XX
- ⚠️ Runtime de la API (PHP / Python / Java / Node…) ≥ XX
- ⚠️ Base de datos (MySQL / PostgreSQL…) ≥ XX
- Git

## Instalación y puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/lucarmendariz/GargantuaProject.git
cd GargantuaProject
```

### 2. Arrancar la API

```bash
cd trazabilidad-api
# ⚠️ COMPLETAR: instalar dependencias
# ⚠️ COMPLETAR: copiar .env.example a .env y configurarlo
# ⚠️ COMPLETAR: crear la base de datos y ejecutar migraciones / importar el script SQL
# ⚠️ COMPLETAR: comando para arrancar el servidor
```

La API quedará disponible en `http://localhost:⚠️PUERTO`.

### 3. Arrancar el frontend

```bash
cd GargantAppFront/gargantapp-front
npm install
npm run dev
```

La aplicación quedará disponible en `http://localhost:⚠️PUERTO`.

## Variables de entorno

Nunca subas archivos `.env` con datos reales. Copia la plantilla y rellénala:

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| ⚠️ `DB_HOST` | Servidor de la base de datos | `localhost` |
| ⚠️ `DB_NAME` | Nombre de la base de datos | `gargantua` |
| ⚠️ `DB_USER` | Usuario de la base de datos | `root` |
| ⚠️ `DB_PASSWORD` | Contraseña | *(no subir al repo)* |
| ⚠️ `API_URL` (frontend) | URL base de la API | `http://localhost:8000/api` |

## Documentación de la API

⚠️ COMPLETAR con tus endpoints reales. Plantilla:

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/login` | Iniciar sesión | No |
| GET | `/api/⚠️recurso` | Listar elementos | Sí |
| GET | `/api/⚠️recurso/{id}` | Detalle de un elemento | Sí |
| POST | `/api/⚠️recurso` | Crear elemento | Sí |
| PUT | `/api/⚠️recurso/{id}` | Modificar elemento | Sí |
| DELETE | `/api/⚠️recurso/{id}` | Eliminar elemento | Admin |

## Tests

```bash
# Backend: necesita Docker, levanta su propia MariaDB temporal
cd trazabilidad-api && ./gradlew test

# Frontend
cd GargantAppFront/gargantapp-front && npm test
```

## Hoja de ruta

- [ ] Completar README con capturas de pantalla
- [ ] Añadir Docker para levantar todo con un solo comando
- [ ] Añadir tests automáticos
- [ ] Documentar la API (OpenAPI / Swagger)
- [ ] ⚠️ COMPLETAR: siguientes funcionalidades

## Autoría

Proyecto desarrollado por **⚠️ tu nombre** como ⚠️ proyecto de ciclo / trabajo personal.

⚠️ COMPLETAR: licencia (por ejemplo, MIT) o indicar que es de uso académico.
