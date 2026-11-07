import { ref } from 'vue';
import { errorMessage } from '../utils/errors';

// Acciones de una pantalla: evita dobles clics, muestra el resultado y vuelve a cargar los datos.
export function useAction(reload) {
  const busy = ref(false);
  const actionError = ref('');
  const actionSuccess = ref('');

  const run = async (action, success, fallback) => {
    if (busy.value) return;
    busy.value = true;
    actionError.value = '';
    actionSuccess.value = '';
    try {
      await action();
      actionSuccess.value = success;
    } catch (error) {
      actionError.value = await errorMessage(error, fallback);
    } finally {
      // También tras un error: la pantalla vuelve a mostrar lo que de verdad está guardado
      try {
        await reload();
      } finally {
        busy.value = false;
      }
    }
  };

  return { busy, actionError, actionSuccess, run };
}
