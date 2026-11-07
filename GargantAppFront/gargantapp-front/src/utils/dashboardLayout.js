// Orden de los bloques del Panel Central, personalizable por cada usuario.
// Se guarda en el navegador: cada usuario mantiene su orden al volver a entrar.

export const DEFAULT_LAYOUT = [
  'calendario',
  'pedidos',
  'inventario',
  'lotes',
  'historial',
  'produccion',
  'administracion',
  'plataforma',
];

const storageKey = (username) => `dashboardLayout:${username}`;

// Respeta el orden guardado, quita bloques que ya no existen y añade al final los nuevos.
export function normalizeLayout(saved, defaults = DEFAULT_LAYOUT) {
  const known = Array.isArray(saved) ? saved.filter((id, index) => defaults.includes(id) && saved.indexOf(id) === index) : [];
  return [...known, ...defaults.filter((id) => !known.includes(id))];
}

export function loadLayout(username) {
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey(username)));
    return normalizeLayout(saved);
  } catch {
    return [...DEFAULT_LAYOUT];
  }
}

export function saveLayout(username, layout) {
  try {
    localStorage.setItem(storageKey(username), JSON.stringify(layout));
  } catch {
    // Sin almacenamiento (modo privado) el orden dura solo esta visita
  }
}

export function resetLayout(username) {
  try {
    localStorage.removeItem(storageKey(username));
  } catch {
    // Nada que borrar
  }
  return [...DEFAULT_LAYOUT];
}

// Devuelve una copia con el bloque de la posición "from" movido a la posición "to".
export function moveItem(list, from, to) {
  if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return [...list];
  const copy = [...list];
  const [item] = copy.splice(from, 1);
  copy.splice(to, 0, item);
  return copy;
}
