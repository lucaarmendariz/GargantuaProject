import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import basicSsl from '@vitejs/plugin-basic-ssl'

// HTTPS=true sirve la app por HTTPS: Safari en iPhone solo deja usar la cámara así.
// Si existe el certificado de scripts/generar-certificado.sh (con la IP del PC y una
// CA que se instala en el móvil) se usa ese; si no, uno provisional solo para localhost.
const https = process.env.HTTPS === 'true'
const certFile = fileURLToPath(new URL('./certs/servidor.crt', import.meta.url))
const keyFile = fileURLToPath(new URL('./certs/servidor.key', import.meta.url))
const ownCertificate = https && existsSync(certFile) && existsSync(keyFile)
const caFile = fileURLToPath(new URL('./certs/gargantua-ca.crt', import.meta.url))

// /gargantua-ca.crt descarga la CA con el tipo que el iPhone reconoce como perfil instalable
const caDownload = {
  name: 'gargantua-ca-download',
  configureServer(server) {
    server.middlewares.use('/gargantua-ca.crt', (_req, res) => {
      if (!existsSync(caFile)) {
        res.statusCode = 404
        res.end('Primero ejecuta scripts/generar-certificado.sh')
        return
      }
      res.setHeader('Content-Type', 'application/x-x509-ca-cert')
      res.setHeader('Content-Disposition', 'attachment; filename="gargantua-ca.crt"')
      res.end(readFileSync(caFile))
    })
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    caDownload,
    ...(https && !ownCertificate ? [basicSsl()] : []),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    https: ownCertificate ? { cert: readFileSync(certFile), key: readFileSync(keyFile) } : undefined,
    // El navegador pide /api al mismo servidor del front y Vite se lo pasa a la API.
    // Así funciona igual desde el PC (localhost) que desde un móvil en la misma WiFi.
    proxy: {
      '/api': {
        target: process.env.API_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        // Para el navegador /api es el mismo sitio que la app. Sin quitar el Origin,
        // la API aplicaría CORS y rechazaría (403) cualquier origen distinto de
        // http://localhost:5173, como https://<IP del PC>:5173 desde el móvil.
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        },
      },
    },
  },
})
