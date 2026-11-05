import { describe, expect, it } from 'vitest';
import { eventToTimeline, historyToTimeline } from '../utils/timeline';

describe('línea de tiempo', () => {
  it('incluye el pedido en los movimientos del lote', () => {
    const item = historyToTimeline({ historyId: 1, actionType: 'CONSUMIDO', actionTimestamp: 1800000000, operator: 'Ana', orderNumber: 'PED-1', quantityAffected: 0, observations: null, ocrRawText: null });
    expect(item.title).toBe('CONSUMIDO');
    expect(item.rows).toContainEqual({ label: 'Pedido', value: 'PED-1' });
    expect(item.rows).toContainEqual({ label: 'Cantidad afectada', value: 0 });
    expect(item.rows.some((row) => row.label === 'Observaciones')).toBe(false);
  });

  it('muestra la cantidad con su unidad en los eventos del pedido', () => {
    const item = eventToTimeline({ eventId: 2, eventType: 'RESERVADO', eventTimestamp: 1800000000, operator: 'Ana', lotNumber: 'L-1', quantity: 2.5, measureUnit: 'KILOS', notes: null });
    expect(item.rows).toContainEqual({ label: 'Cantidad', value: '2.5 KILOS' });
    expect(item.rows).toContainEqual({ label: 'Lote', value: 'L-1' });
  });
});
