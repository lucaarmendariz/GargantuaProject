// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import router from '../router';
import { can } from '../utils/session';

// Permisos por defecto de cada rol (03_seeds.sql); los de ADMIN y SUPERADMIN, todos
const DEFAULT_ACCESS = {
  OPERARIO: { PEDIDOS: 'VER', PRODUCCION: 'VER' },
  ENCARGADO: { PEDIDOS: 'GESTIONAR', INVENTARIO: 'GESTIONAR', LOTES: 'GESTIONAR' },
};
const ALL = ['PEDIDOS', 'INVENTARIO', 'LOTES', 'HISTORIAL', 'PRODUCCION'];

const login = (role, access = DEFAULT_ACCESS[role]) => {
  const modules = ALL.map((moduleCode) => ({
    moduleCode,
    access: access ? access[moduleCode] || 'NINGUNO' : 'GESTIONAR',
  }));
  localStorage.setItem('token', 'abc');
  localStorage.setItem('user', JSON.stringify({ username: 't', name: 'T', role }));
  localStorage.setItem('access', JSON.stringify(modules));
};
const goTo = async (path) => {
  await router.push(path);
  return router.currentRoute.value.path;
};

describe('guard del router', () => {
  beforeEach(async () => {
    localStorage.clear();
    vi.stubGlobal('alert', vi.fn());
    // Sin API: el guard usa los permisos guardados
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('sin conexión')));
    await router.push('/');
  });

  it('sin sesión vuelve al login', async () => {
    expect(await goTo('/dashboard')).toBe('/');
  });

  it('un usuario sin token vuelve al login', async () => {
    localStorage.setItem('user', '{"role":"ADMIN"}');
    expect(await goTo('/dashboard')).toBe('/');
  });

  it('cada pantalla se abre solo con permiso en su módulo', async () => {
    login('OPERARIO');
    expect(await goTo('/produccion')).toBe('/produccion');
    expect(await goTo('/historial-global')).toBe('/dashboard');
    expect(alert).toHaveBeenCalledOnce();
  });

  it('el operario ve pedidos y la trazabilidad de los lotes', async () => {
    login('OPERARIO');
    expect(await goTo('/pedidos')).toBe('/pedidos');
    expect(await goTo('/pedidos/5')).toBe('/pedidos/5');
    expect(await goTo('/lotes/3/trazabilidad')).toBe('/lotes/3/trazabilidad');
  });

  it('la ruta antigua de pedidos cerrados abre su pestaña en Pedidos', async () => {
    login('OPERARIO');
    expect(await goTo('/pedidos/cerrados')).toBe('/pedidos');
    expect(router.currentRoute.value.query.tab).toBe('cerrados');
  });

  it('el administrador entra en el historial', async () => {
    login('ADMIN');
    expect(await goTo('/historial-global')).toBe('/historial-global');
  });

  it('si el administrador del cliente da permiso, el operario entra en lotes', async () => {
    login('OPERARIO');
    expect(await goTo('/lotes')).toBe('/dashboard');
    login('OPERARIO', { LOTES: 'VER' });
    expect(await goTo('/lotes')).toBe('/lotes');
  });

  it('un módulo no contratado no se abre ni para el administrador', async () => {
    login('ADMIN', { PEDIDOS: 'GESTIONAR' });
    expect(await goTo('/inventario')).toBe('/dashboard');
  });

  it('cada panel se abre solo para su rol', async () => {
    login('ENCARGADO');
    expect(await goTo('/administracion')).toBe('/dashboard');
    login('ADMIN');
    expect(await goTo('/administracion')).toBe('/administracion');
    expect(await goTo('/plataforma')).toBe('/dashboard');
    login('SUPERADMIN');
    expect(await goTo('/plataforma')).toBe('/plataforma');
  });
});

describe('permisos por nivel', () => {
  it('GESTIONAR incluye VER, pero VER no incluye GESTIONAR', () => {
    login('OPERARIO', { PEDIDOS: 'VER', LOTES: 'GESTIONAR' });
    expect(can('PEDIDOS')).toBe(true);
    expect(can('PEDIDOS', 'GESTIONAR')).toBe(false);
    expect(can('LOTES', 'VER')).toBe(true);
    expect(can('HISTORIAL')).toBe(false);
  });
});
