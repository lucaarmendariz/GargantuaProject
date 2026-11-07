<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { useAction } from '../composables/useAction';
import { refreshAccess } from '../utils/access';
import { errorMessage } from '../utils/errors';

// Panel del proveedor (SUPERADMIN): qué módulos tiene contratados este cliente.
const router = useRouter();
const modules = ref([]);
const loadError = ref('');

const load = async () => {
  try {
    modules.value = await apiClient.get('/plataforma/modulos');
    loadError.value = '';
  } catch (error) {
    loadError.value = await errorMessage(error, 'No se pudieron cargar los módulos.');
  }
  // Los permisos de todos (también los tuyos) dependen de esto
  await refreshAccess({ force: true });
};

const { busy, actionError, actionSuccess, run } = useAction(load);

const toggle = (module) => run(
  () => apiClient.put(`/plataforma/modulos/${module.moduleCode}`, { enabled: !module.enabled }),
  `${module.name}: ${module.enabled ? 'desactivado' : 'activado'}.`,
  'No se pudo cambiar el módulo.'
);

onMounted(load);
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Plataforma</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p class="item-meta">
        Activa los módulos que este cliente tiene contratados. Un módulo desactivado desaparece para todos
        sus usuarios, también para su administrador, y la API rechaza cualquier acceso a él.
      </p>

      <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
      <p v-if="actionError || loadError" class="msg-error" role="alert">{{ actionError || loadError }}</p>

      <section class="panel">
        <h3>Módulos contratados</h3>
        <ul class="module-list">
          <li v-for="module in modules" :key="module.moduleCode" class="item-card module-row">
            <div>
              <span class="item-title">{{ module.name }}</span>
              <span class="item-meta"><code>{{ module.moduleCode }}</code></span>
            </div>
            <label class="switch">
              <input type="checkbox" role="switch" :checked="module.enabled" :disabled="busy"
                :aria-label="`${module.name} contratado`" @change="toggle(module)" />
              <span>{{ module.enabled ? 'Contratado' : 'No contratado' }}</span>
            </label>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 14px;
}

.module-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.module-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  text-align: left;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.module-row > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.switch {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: var(--control-height);
  font-weight: 600;
  cursor: pointer;
}

.switch input {
  width: 22px;
  height: 22px;
  accent-color: var(--gargantua-orange);
}
</style>
