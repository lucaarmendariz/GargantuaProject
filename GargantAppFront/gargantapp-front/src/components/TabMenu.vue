<script setup>
import { nextTick, ref } from 'vue';
import { neighbourTab } from '../utils/tabs';

// Menú de pestañas de un módulo: cada pestaña muestra una única acción.
// Uso: <TabMenu v-model="tab" :tabs="[{ id: 'ver', label: 'Ver lotes' }]" label="Acciones de lotes" />
// El contenido de cada pestaña va en un elemento con id="panel-<id>" y aria-labelledby="tab-<id>".
const props = defineProps({
  tabs: { type: Array, required: true },
  modelValue: { type: String, required: true },
  label: { type: String, required: true },
});

const emit = defineEmits(['update:modelValue']);
const buttons = ref({});

const select = async (id) => {
  emit('update:modelValue', id);
  await nextTick();
  buttons.value[id]?.focus();
};

const onKeydown = (event) => {
  const keys = { ArrowRight: 1, ArrowLeft: -1 };
  if (event.key in keys) {
    event.preventDefault();
    select(neighbourTab(props.tabs, props.modelValue, keys[event.key]));
  } else if (event.key === 'Home' || event.key === 'End') {
    event.preventDefault();
    select(props.tabs[event.key === 'Home' ? 0 : props.tabs.length - 1].id);
  }
};
</script>

<template>
  <nav class="tab-menu" :aria-label="label">
    <div role="tablist" :aria-label="label" class="tab-list" @keydown="onKeydown">
      <button v-for="tab in tabs" :key="tab.id" :ref="(el) => { buttons[tab.id] = el; }"
        :id="`tab-${tab.id}`" type="button" role="tab" class="tab"
        :class="{ 'tab-active': tab.id === modelValue }"
        :aria-selected="tab.id === modelValue" :aria-controls="`panel-${tab.id}`"
        :tabindex="tab.id === modelValue ? 0 : -1" @click="select(tab.id)">
        {{ tab.label }}
      </button>
    </div>
  </nav>
</template>

<style scoped>
.tab-menu {
  border-bottom: 1px solid var(--gargantua-border);
}

.tab-list {
  display: flex;
  gap: 4px;
  margin-bottom: -1px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.tab {
  flex: 0 0 auto;
  min-height: var(--control-height);
  padding: 8px 16px;
  background: transparent;
  border: 1px solid transparent;
  border-bottom: 3px solid transparent;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  color: var(--gargantua-gray);
  font-weight: 600;
  white-space: nowrap;
}

.tab:hover {
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
}

.tab-active,
.tab-active:hover {
  background: var(--gargantua-white);
  border-color: var(--gargantua-border);
  border-bottom-color: var(--gargantua-orange);
  color: var(--gargantua-orange-dark);
}

.tab:focus-visible {
  outline: 2px solid var(--gargantua-orange);
  outline-offset: -2px;
}

/* En el móvil las pestañas pasan a botones que bajan de línea: se ven todas sin desplazar */
@media (max-width: 600px) {
  .tab-menu {
    border-bottom: 0;
  }

  .tab-list {
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 0;
    overflow: visible;
  }

  .tab {
    flex: 1 1 auto;
    padding-inline: 12px;
    border-color: var(--gargantua-border);
    border-bottom-width: 1px;
    border-radius: var(--radius-sm);
    white-space: normal;
  }

  .tab-active,
  .tab-active:hover {
    border-color: var(--gargantua-orange);
    border-bottom-color: var(--gargantua-orange);
    box-shadow: inset 0 -3px 0 var(--gargantua-orange);
  }
}
</style>
