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
