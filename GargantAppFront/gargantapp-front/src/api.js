import { clearSession, getToken } from './utils/session';

// Ruta relativa: el servidor de Vite reenvía /api a la API (ver vite.config.ts).
// Así la app funciona tanto en el PC como desde un móvil de la misma red.
const BASE_URL = import.meta.env.VITE_API_URL || '/api';

// El login no lleva token: uno caducado haría que el backend rechazara la petición.
function authHeaders(endpoint) {
  const token = getToken();
  return token && !endpoint.startsWith('/auth/') ? { Authorization: `Bearer ${token}` } : {};
}

// Lanza la respuesta si es un error (404, 500...) y lee el cuerpo según su tipo:
// el backend puede devolver JSON, texto plano o nada (204).
async function parseResponse(response, endpoint) {
  // Sesión caducada o inválida: se cierra y se vuelve al login
  if (response.status === 401 && !endpoint.startsWith('/auth/')) {
    clearSession();
    window.location.assign('/');
  }
  if (!response.ok) throw response;
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  return contentType.includes('application/json') ? response.json() : response.text();
}

async function sendJson(method, endpoint, body) {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(endpoint)
    },
    body: JSON.stringify(body)
  });
  return parseResponse(response, endpoint);
}

const apiClient = {
  // Para peticiones GET (como buscar un lote)
  async get(endpoint) {
    const response = await fetch(`${BASE_URL}${endpoint}`, { headers: authHeaders(endpoint) });
    return parseResponse(response, endpoint);
  },

  // Para peticiones POST (como el Login)
  post(endpoint, body) {
    return sendJson('POST', endpoint, body);
  },

  // Para peticiones PUT (como editar un producto)
  put(endpoint, body) {
    return sendJson('PUT', endpoint, body);
  },

  // Para peticiones DELETE (como dar de baja un producto)
  async delete(endpoint) {
    const response = await fetch(`${BASE_URL}${endpoint}`, { method: 'DELETE', headers: authHeaders(endpoint) });
    return parseResponse(response, endpoint);
  }
};

export default apiClient;