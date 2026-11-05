import { describe, expect, it } from 'vitest';
import { errorMessage } from '../utils/errors';

describe('errorMessage', () => {
  it('muestra el motivo enviado por el backend', async () => {
    const response = new Response('No hay stock disponible suficiente en el lote.', { status: 409 });
    expect(await errorMessage(response, 'Fallo')).toBe('No hay stock disponible suficiente en el lote.');
  });

  it('no muestra respuestas técnicas', async () => {
    expect(await errorMessage(new Response('{"status":404}', { status: 404 }), 'Fallo')).toBe('Fallo');
    expect(await errorMessage(new Response('Traza interna', { status: 500 }), 'Fallo')).toBe('Fallo');
  });

  it('avisa de permisos y de falta de conexión', async () => {
    expect(await errorMessage(new Response('', { status: 403 }), 'Fallo')).toContain('permisos');
    expect(await errorMessage(new TypeError('Failed to fetch'), 'Fallo')).toContain('conectar');
  });
});
