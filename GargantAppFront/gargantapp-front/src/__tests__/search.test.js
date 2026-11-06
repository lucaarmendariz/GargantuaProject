import { describe, expect, it } from 'vitest';
import { matchesSearch, normalizeText } from '../utils/search';

describe('búsqueda de texto', () => {
  it('ignora mayúsculas y tildes', () => {
    expect(normalizeText('  Sociedad GASTRONÓMICA ')).toBe('sociedad gastronomica');
    expect(matchesSearch('gastronomica', ['Sociedad Gastronómica Lagunak'])).toBe(true);
  });

  it('exige todas las palabras, aunque estén en campos distintos', () => {
    const fields = ['PED-DEMO-004', 'Restaurante Arrantzale', null];
    expect(matchesSearch('demo arrantzale', fields)).toBe(true);
    expect(matchesSearch('demo lagunak', fields)).toBe(false);
  });

  it('sin texto lo muestra todo', () => {
    expect(matchesSearch('   ', ['x'])).toBe(true);
  });
});
