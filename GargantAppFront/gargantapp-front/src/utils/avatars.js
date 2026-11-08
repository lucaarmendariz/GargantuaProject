import { reactive } from 'vue';
import apiClient from '../api';

// Fotos de quien firma los movimientos: se piden una sola vez por usuario mientras la página esté abierta
const avatars = reactive({});
const requested = new Set();

export function avatarOf(username) {
  if (username && !requested.has(username)) {
    requested.add(username);
    apiClient.get(`/usuarios/${encodeURIComponent(username)}/foto`)
      .then((body) => { avatars[username] = body?.avatar || null; })
      .catch(() => { requested.delete(username); });
  }
  return avatars[username] || null;
}

// Al cambiar la propia foto, para que se vea ya en los movimientos sin recargar
export function setAvatar(username, avatar) {
  requested.add(username);
  avatars[username] = avatar || null;
}
