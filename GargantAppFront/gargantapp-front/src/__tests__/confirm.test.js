import { describe, expect, it } from 'vitest';
import { answerConfirm, askConfirm, confirmState } from '../utils/confirm';

describe('ventana de confirmación propia', () => {
  it('abre la pregunta y devuelve lo que elige el usuario', async () => {
    const answer = askConfirm({ title: '¿Dar de baja el lote L1?', confirmText: 'Dar de baja', danger: true });
    expect(confirmState).toMatchObject({ open: true, title: '¿Dar de baja el lote L1?', danger: true });
    answerConfirm(true);
    await expect(answer).resolves.toBe(true);
    expect(confirmState.open).toBe(false);

    const no = askConfirm({ title: '¿Seguro?' });
    answerConfirm(false);
    await expect(no).resolves.toBe(false);
  });

  it('una pregunta nueva da por cancelada la que estaba abierta', async () => {
    const first = askConfirm({ title: 'Primera' });
    const second = askConfirm({ title: 'Segunda' });
    await expect(first).resolves.toBe(false);
    answerConfirm(true);
    await expect(second).resolves.toBe(true);
  });
});
