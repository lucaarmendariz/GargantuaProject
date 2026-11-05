import { formatEpochDateTime } from './date';

const row = (label, value) => ({ label, value });

// Movimiento de un lote convertido al formato de EventTimeline.
export function historyToTimeline(log) {
  return {
    key: log.historyId,
    title: log.actionType,
    time: formatEpochDateTime(log.actionTimestamp),
    rows: [
      row('Operario', log.operator),
      log.orderNumber ? row('Pedido', log.orderNumber) : null,
      log.quantityAffected !== null && log.quantityAffected !== undefined ? row('Cantidad afectada', log.quantityAffected) : null,
      log.observations ? row('Observaciones', log.observations) : null,
    ].filter(Boolean),
    code: log.ocrRawText,
  };
}

// Evento de un pedido convertido al formato de EventTimeline.
export function eventToTimeline(event) {
  const quantity = event.quantity !== null && event.quantity !== undefined
    ? `${event.quantity} ${event.measureUnit || ''}`.trim()
    : null;
  return {
    key: event.eventId,
    title: event.eventType,
    time: formatEpochDateTime(event.eventTimestamp),
    rows: [
      row('Operario', event.operator),
      event.lotNumber ? row('Lote', event.lotNumber) : null,
      quantity ? row('Cantidad', quantity) : null,
      event.notes ? row('Observaciones', event.notes) : null,
    ].filter(Boolean),
  };
}
