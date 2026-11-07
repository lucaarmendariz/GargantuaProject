import apiClient from '../api';
import { saveAccess } from './session';

let loaded = false;

// Lee de la API los permisos del usuario conectado. Se llama al entrar y una vez por cada carga de la
// página, así los cambios del panel del administrador llegan al recargar. Si falla se siguen usando
// los guardados (si la sesión caducó, api.js ya devuelve al login).
export async function refreshAccess({ force = false } = {}) {
  if (loaded && !force) return;
  try {
    saveAccess(await apiClient.get('/me/permisos'));
    loaded = true;
  } catch {
    // Sin conexión: se mantienen los permisos guardados
  }
}
