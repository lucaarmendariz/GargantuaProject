// El backend explica en texto por qué rechaza una operación de pedidos.
export async function errorMessage(error, fallback) {
  if (typeof error?.text !== 'function') {
    return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
  if (error.status === 403) {
    return 'Tu perfil no tiene permisos para esta acción.';
  }
  if (error.status >= 500) {
    return fallback;
  }
  try {
    const text = await error.text();
    return text && !text.trim().startsWith('{') ? text : fallback;
  } catch {
    return fallback;
  }
}
