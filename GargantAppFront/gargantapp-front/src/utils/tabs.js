// Pestaña activa de un módulo a partir del parámetro ?tab= de la URL.
// Si falta o no es una de las pestañas disponibles (p. ej. por el rol), se usa la primera.
export function resolveTab(queryTab, tabs) {
  const ids = tabs.map((tab) => tab.id);
  return ids.includes(queryTab) ? queryTab : ids[0];
}

// Pestaña a la que se pasa con las flechas del teclado, dando la vuelta en los extremos.
export function neighbourTab(tabs, currentId, step) {
  const index = tabs.findIndex((tab) => tab.id === currentId);
  if (index === -1 || tabs.length === 0) return tabs[0]?.id;
  return tabs[(index + step + tabs.length) % tabs.length].id;
}
