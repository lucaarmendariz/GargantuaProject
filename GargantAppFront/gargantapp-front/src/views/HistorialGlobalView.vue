<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { formatEpochDateTime } from '../utils/date';

const lotIdInput = ref('');
const historyLogs = ref([]);
const searchDone = ref(false);
const errorMsg = ref('');
const router = useRouter();

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

      <div v-if="historyLogs.length > 0" class="timeline">
        <div v-for="log in historyLogs" :key="log.historyId" class="timeline-item">
          <div class="timeline-badge" :class="log.actionType.toLowerCase()"></div>
          <div class="timeline-panel">
            <div class="timeline-heading">
              <span class="action-title">{{ log.actionType }}</span>
              <span class="action-time">{{ formatEpochDateTime(log.actionTimestamp) }}</span>
            </div>
            <div class="timeline-body">
              <p><strong>Operario en planta:</strong> {{ log.operator }}</p>
              <p v-if="log.quantityAffected"><strong>Cantidad afectada:</strong> {{ log.quantityAffected }}</p>
              <p><strong>Observaciones registradas:</strong> {{ log.observations || 'Ninguna' }}</p>
              <div class="ocr-dump">
                <code>{{ log.ocrRawText }}</code>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <p v-else-if="searchDone && !errorMsg" class="info-empty">Este lote existe pero no tiene movimientos registrados.</p>
    </div>
  </div>
</template>

<style scoped>
.info-empty {
  color: var(--gargantua-gray);
  text-align: center;
}

.timeline {
  position: relative;
}

.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 6px;
  width: 2px;
  background: var(--gargantua-border);
}

.timeline-item {
  position: relative;
  display: flex;
  gap: clamp(12px, 3vw, 20px);
  margin-bottom: 16px;
}

.timeline-badge {
  z-index: 1;
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  margin-top: 18px;
  background: var(--gargantua-gold);
  border: 2px solid var(--gargantua-dark);
  border-radius: 50%;
}

.timeline-panel {
  flex: 1;
  min-width: 0;
  padding: 16px;
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.timeline-heading {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 2px 12px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--gargantua-border);
}

.action-title {
  color: var(--gargantua-dark);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.action-time {
  color: var(--gargantua-gray);
  font-size: 0.85rem;
}

.timeline-body p {
  margin: 4px 0;
  font-size: 0.925rem;
  overflow-wrap: anywhere;
}

.timeline-body strong {
  font-weight: 600;
}

.ocr-dump {
  margin-top: 8px;
  padding: 6px 10px;
  background: var(--gargantua-cream);
  border-radius: var(--radius-sm);
}
</style>
