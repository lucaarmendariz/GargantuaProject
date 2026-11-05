import { describe, expect, it } from 'vitest';
import { dateInputToEpoch, formatEpochDate, formatEpochDateTime } from '../utils/date';

describe('fechas en epoch', () => {
  it('interpreta el valor como segundos, no milisegundos', () => {
    const expected = new Date(1779282053 * 1000).toLocaleDateString();
    expect(formatEpochDate(1779282053)).toBe(expected);
    expect(new Date(1779282053 * 1000).getFullYear()).toBe(2026);
  });

  it('muestra N/A cuando no hay fecha', () => {
    expect(formatEpochDate(null)).toBe('N/A');
    expect(formatEpochDateTime(undefined)).toBe('N/A');
  });

  it('convierte el campo de fecha a segundos en hora local', () => {
    const epoch = dateInputToEpoch('2030-01-15');
    const date = new Date(epoch * 1000);
    expect([date.getFullYear(), date.getMonth(), date.getDate()]).toEqual([2030, 0, 15]);
    expect(dateInputToEpoch('')).toBeNull();
  });
});
