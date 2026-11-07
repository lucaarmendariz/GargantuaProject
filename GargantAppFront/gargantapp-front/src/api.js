import { clearSession, getToken } from './utils/session';

// Ruta relativa: el servidor de Vite reenvía /api a la API (ver vite.config.ts).
// Así la app funciona tanto en el PC como desde un móvil de la misma red.
const BASE_URL = import.meta.env.VITE_API_URL || '/api';

// Hace la petición, lanza la respuesta si es un error (404, 500...) y lee el cuerpo según su tipo:
// el backend puede devolver JSON, texto plano o nada (204).
async function request(method, endpoint, body) {
  const isAuth = endpoint.startsWith('/auth/');
  const headers = {};
  // El login no lleva token: uno caducado haría que el backend rechazara la petición.
  const token = getToken();
  if (token && !isAuth) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  // Sesión caducada o inválida: se cierra y se vuelve al login
  if (response.status === 401 && !isAuth) {
    clearSession();
    window.location.assign('/');
  }
  if (!response.ok) throw response;
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  return contentType.includes('application/json') ? response.json() : response.text();
}

const apiClient = {
  get: (endpoint) => request('GET', endpoint),
  post: (endpoint, body) => request('POST', endpoint, body),
  put: (endpoint, body) => request('PUT', endpoint, body),
  delete: (endpoint) => request('DELETE', endpoint)
};

export default apiClient;
