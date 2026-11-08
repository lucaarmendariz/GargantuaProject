import { reactive } from 'vue';

// Ventana de confirmación propia de la app (en lugar del confirm() del navegador).
// Uso: if (!(await askConfirm({ title: '¿Eliminar?', message: '...', confirmText: 'Eliminar', danger: true }))) return;
// La pinta ConfirmDialog.vue, montado una sola vez en App.vue.
export const confirmState = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: 'Confirmar',
  danger: false,
  resolve: null,
});

export function askConfirm({ title, message = '', confirmText = 'Confirmar', danger = false }) {
  // Si había otra pregunta abierta, se da por cancelada
  confirmState.resolve?.(false);
  return new Promise((resolve) => {
    Object.assign(confirmState, { open: true, title, message, confirmText, danger, resolve });
  });
}

export function answerConfirm(accepted) {
  const resolve = confirmState.resolve;
  confirmState.open = false;
  confirmState.resolve = null;
  resolve?.(accepted);
}
