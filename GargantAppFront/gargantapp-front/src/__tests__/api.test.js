// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../api';

const respond = (status, body, type = 'application/json') =>
  new Response(body, { status, headers: type ? { 'content-type': type } : {} });

describe('apiClient', () => {
  let fetchMock;

  beforeEach(() => {
    localStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  it('lee JSON, texto plano y respuestas vacías', async () => {
    fetchMock.mockResolvedValueOnce(respond(200, '{"a":1}'));
    await expect(apiClient.get('/x')).resolves.toEqual({ a: 1 });

    fetchMock.mockResolvedValueOnce(respond(201, 'Guardado', 'text/plain'));
    await expect(apiClient.post('/x', {})).resolves.toBe('Guardado');

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(apiClient.delete('/x')).resolves.toBeNull();
  });

  it('envía PUT y DELETE con el token', async () => {
    localStorage.setItem('token', 'abc');
    fetchMock.mockImplementation(async () => respond(200, '{}'));

    await apiClient.put('/productos/1', { name: 'n' });
    await apiClient.delete('/productos/1');

    expect(fetchMock.mock.calls[0][1].method).toBe('PUT');
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer abc');
    expect(fetchMock.mock.calls[1][1].method).toBe('DELETE');
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer abc');
  });

  it('no envía un token antiguo al hacer login', async () => {
    localStorage.setItem('token', 'caducado');
    fetchMock.mockResolvedValue(respond(200, '{}'));

    await apiClient.post('/auth/login', {});

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined();
  });

  it('una contraseña incorrecta no cierra la sesión ni redirige', async () => {
    localStorage.setItem('token', 'abc');
    fetchMock.mockResolvedValue(respond(401, 'No', 'text/plain'));

    await expect(apiClient.post('/auth/login', {})).rejects.toMatchObject({ status: 401 });

    expect(localStorage.getItem('token')).toBe('abc');
  });

  it('un 401 fuera del login cierra la sesión', async () => {
    localStorage.setItem('token', 'abc');
    localStorage.setItem('user', '{"role":"ADMIN"}');
    fetchMock.mockResolvedValue(respond(401, '', null));
    const assign = vi.spyOn(window.location, 'assign').mockImplementation(() => {});

    await expect(apiClient.get('/productos')).rejects.toMatchObject({ status: 401 });

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(assign).toHaveBeenCalledWith('/');
  });

  it('lanza la respuesta en otros errores sin cerrar la sesión', async () => {
    localStorage.setItem('token', 'abc');
    fetchMock.mockResolvedValue(respond(404, '', null));

    await expect(apiClient.get('/lote')).rejects.toMatchObject({ status: 404 });

    expect(localStorage.getItem('token')).toBe('abc');
  });
});
