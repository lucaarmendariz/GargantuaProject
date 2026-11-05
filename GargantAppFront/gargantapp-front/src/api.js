const BASE_URL = 'http://localhost:8080/api';

// Lanza la respuesta si es un error (404, 500...) y lee el cuerpo según su tipo:
// el backend puede devolver JSON, texto plano o nada (204).
async function parseResponse(response) {
  if (!response.ok) throw response;
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  return contentType.includes('application/json') ? response.json() : response.text();
}

async function sendJson(method, endpoint, body) {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    method,
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  return parseResponse(response);
}

const apiClient = {
  // Para peticiones GET (como buscar un lote)
  async get(endpoint) {
    const response = await fetch(`${BASE_URL}${endpoint}`);
    return parseResponse(response);
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
    const response = await fetch(`${BASE_URL}${endpoint}`, { method: 'DELETE' });
    return parseResponse(response);
  }
};

export default apiClient;