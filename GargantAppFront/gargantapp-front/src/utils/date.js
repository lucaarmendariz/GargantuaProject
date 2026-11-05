// Las fechas viajan como epoch en segundos y se muestran en la hora local del navegador.

export function formatEpochDateTime(seconds) {
  if (seconds === null || seconds === undefined) return 'N/A';
  return new Date(seconds * 1000).toLocaleString();
}

export function formatEpochDate(seconds) {
  if (seconds === null || seconds === undefined) return 'N/A';
  return new Date(seconds * 1000).toLocaleDateString();
}

// Convierte el valor de un <input type="date"> (AAAA-MM-DD) a epoch en segundos.
export function dateInputToEpoch(value) {
  if (!value) return null;
  return Math.floor(new Date(`${value}T00:00:00`).getTime() / 1000);
}

// Valor de hoy para un <input type="date">, en hora local.
export function todayInputValue() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

// Inicio del día indicado y del siguiente, en epoch. Sirve también en los cambios de hora.
export function dayRange(date) {
  const from = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const to = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
  return { from: from.getTime() / 1000, to: to.getTime() / 1000 };
}

export function formatDayLong(date) {
  return date.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

export function isExpired(seconds, nowMs = Date.now()) {
  return seconds !== null && seconds !== undefined && seconds * 1000 < nowMs;
}
