import { describe, expect, it } from 'vitest';
import { dateInputToEpoch, dayRange, formatEpochDate, formatEpochDateTime, isExpired, todayInputValue } from '../utils/date';

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

  it('calcula el día completo en hora local, incluso con cambio de hora', () => {
    const { from, to } = dayRange(new Date(2026, 9, 25, 15, 30));
    expect(new Date(from * 1000).getHours()).toBe(0);
    expect(new Date(from * 1000).getDate()).toBe(25);
    expect(new Date(to * 1000).getDate()).toBe(26);
    expect(new Date(to * 1000).getHours()).toBe(0);
  });

  it('genera la fecha de hoy para el formulario', () => {
    const now = new Date();
    expect(dateInputToEpoch(todayInputValue())).toBe(dayRange(now).from);
  });

  it('detecta los lotes caducados', () => {
    const now = 1800000000000;
    expect(isExpired(1700000000, now)).toBe(true);
    expect(isExpired(1900000000, now)).toBe(false);
    expect(isExpired(null, now)).toBe(false);
  });
});
