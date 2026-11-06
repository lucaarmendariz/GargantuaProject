// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from 'vitest';
import { DEFAULT_LAYOUT, loadLayout, moveItem, normalizeLayout, resetLayout, saveLayout } from '../utils/dashboardLayout';

describe('orden del Panel Central', () => {
  beforeEach(() => localStorage.clear());

  it('mueve un bloque a otra posición sin tocar la lista original', () => {
    const list = ['a', 'b', 'c', 'd'];
    expect(moveItem(list, 0, 2)).toEqual(['b', 'c', 'a', 'd']);
    expect(moveItem(list, 3, 0)).toEqual(['d', 'a', 'b', 'c']);
    expect(moveItem(list, 1, 9)).toEqual(list);
    expect(list).toEqual(['a', 'b', 'c', 'd']);
  });

  it('limpia lo guardado: quita bloques desconocidos o repetidos y añade los nuevos', () => {
    expect(normalizeLayout(['lotes', 'viejo', 'lotes', 'calendario'])).toEqual([
      'lotes', 'calendario', ...DEFAULT_LAYOUT.filter((id) => !['lotes', 'calendario'].includes(id)),
    ]);
    expect(normalizeLayout(null)).toEqual(DEFAULT_LAYOUT);
  });

  it('guarda el orden de cada usuario por separado', () => {
    const custom = moveItem(DEFAULT_LAYOUT, 0, DEFAULT_LAYOUT.length - 1);
    saveLayout('ane', custom);
    expect(loadLayout('ane')).toEqual(custom);
    expect(loadLayout('mikel')).toEqual(DEFAULT_LAYOUT);
    expect(resetLayout('ane')).toEqual(DEFAULT_LAYOUT);
    expect(loadLayout('ane')).toEqual(DEFAULT_LAYOUT);
  });

  it('si lo guardado está corrupto vuelve al orden por defecto', () => {
    localStorage.setItem('dashboardLayout:ane', '{no es json');
    expect(loadLayout('ane')).toEqual(DEFAULT_LAYOUT);
  });
});
