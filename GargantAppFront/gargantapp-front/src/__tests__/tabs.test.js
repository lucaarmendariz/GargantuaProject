import { describe, expect, it } from 'vitest';
import { neighbourTab, resolveTab } from '../utils/tabs';
import { DEFAULT_LAYOUT, normalizeLayout } from '../utils/dashboardLayout';

const tabs = [{ id: 'en-curso' }, { id: 'nuevo' }, { id: 'cerrados' }];

describe('menú de pestañas de los módulos', () => {
  it('abre la pestaña indicada en la URL', () => {
    expect(resolveTab('cerrados', tabs)).toBe('cerrados');
  });

  it('sin pestaña, o con una que no existe o no está disponible, abre la primera', () => {
    expect(resolveTab(undefined, tabs)).toBe('en-curso');
    expect(resolveTab('inventada', tabs)).toBe('en-curso');
    // Un operario no tiene la pestaña "nuevo"
    expect(resolveTab('nuevo', tabs.filter((tab) => tab.id !== 'nuevo'))).toBe('en-curso');
  });

  it('las flechas pasan a la pestaña de al lado y dan la vuelta en los extremos', () => {
    expect(neighbourTab(tabs, 'en-curso', 1)).toBe('nuevo');
    expect(neighbourTab(tabs, 'cerrados', 1)).toBe('en-curso');
    expect(neighbourTab(tabs, 'en-curso', -1)).toBe('cerrados');
  });
});

describe('Pedidos cerrados dentro de Pedidos', () => {
  it('ya no es un módulo propio del Panel Central', () => {
    expect(DEFAULT_LAYOUT).not.toContain('pedidos-cerrados');
  });

  it('se quita del orden que tenga guardado cada usuario', () => {
    expect(normalizeLayout(['pedidos-cerrados', 'lotes', 'pedidos'])).not.toContain('pedidos-cerrados');
  });
});
