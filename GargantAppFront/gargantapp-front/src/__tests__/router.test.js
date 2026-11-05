// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import router from '../router';

const login = (role) => {
  localStorage.setItem('token', 'abc');
  localStorage.setItem('user', JSON.stringify({ username: 't', name: 'T', role }));
};
const goTo = async (path) => {
  await router.push(path);
  return router.currentRoute.value.path;
};

describe('guard del router', () => {
  beforeEach(async () => {
    localStorage.clear();
    vi.stubGlobal('alert', vi.fn());
    await router.push('/');
  });

  it('sin sesión vuelve al login', async () => {
    expect(await goTo('/dashboard')).toBe('/');
  });

  it('un usuario sin token vuelve al login', async () => {
    localStorage.setItem('user', '{"role":"ADMIN"}');
    expect(await goTo('/dashboard')).toBe('/');
  });

  it('respeta los roles de cada pantalla', async () => {
    login('OPERARIO');
    expect(await goTo('/produccion')).toBe('/produccion');
    expect(await goTo('/historial-global')).toBe('/dashboard');
    expect(alert).toHaveBeenCalledOnce();
  });

  it('el administrador entra en el historial', async () => {
    login('ADMIN');
    expect(await goTo('/historial-global')).toBe('/historial-global');
  });
});
