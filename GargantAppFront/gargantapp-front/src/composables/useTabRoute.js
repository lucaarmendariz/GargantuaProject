import { computed, unref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { resolveTab } from '../utils/tabs';

// Guarda la pestaña activa en la URL (?tab=...): sobrevive a recargar la página y
// los enlaces y el botón Atrás del navegador llevan a la pestaña correcta.
// "tabs" puede ser un array o un computed (pestañas que dependen del rol o del estado).
export function useTabRoute(tabs) {
  const route = useRoute();
  const router = useRouter();

  return computed({
    get: () => resolveTab(route.query.tab, unref(tabs)),
    set: (tab) => {
      if (tab === route.query.tab) return;
      router.replace({ query: { ...route.query, tab } });
    },
  });
}
