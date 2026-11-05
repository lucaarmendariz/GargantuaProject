<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import EventTimeline from '../components/EventTimeline.vue';
import { historyToTimeline } from '../utils/timeline';

const lotIdInput = ref('');
const historyLogs = ref([]);
const searchDone = ref(false);
const errorMsg = ref('');
const router = useRouter();

const timelineItems = computed(() => historyLogs.value.map(historyToTimeline));

const fetchHistory = async () => {
  if (!lotIdInput.value) return;
  try {
    errorMsg.value = '';
    historyLogs.value = await apiClient.get(`/historial/lote/${lotIdInput.value}`);
    searchDone.value = true;
  } catch (error) {
    historyLogs.value = [];
    searchDone.value = true;
    errorMsg.value = error?.status === 404
      ? 'No existe ningún lote con este ID.'
      : 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Visibilizar Historial de Auditoría</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <div class="input-row">
        <input v-model="lotIdInput" type="number" placeholder="Introduce el ID numérico del lote (Ej: 1, 2)..." @keyup.enter="fetchHistory" />
        <button @click="fetchHistory">Ver Auditoría</button>
      </div>

      <p v-if="errorMsg" class="msg-error">{{ errorMsg }}</p>

      <EventTimeline v-if="historyLogs.length > 0" :items="timelineItems" />

      <p v-else-if="searchDone && !errorMsg" class="info-empty">Este lote existe pero no tiene movimientos registrados.</p>
    </div>
  </div>
</template>

<style scoped>
.info-empty {
  color: var(--gargantua-gray);
  text-align: center;
}

</style>
