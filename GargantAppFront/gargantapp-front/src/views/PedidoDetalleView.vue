<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import EventTimeline from '../components/EventTimeline.vue';
import { formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { getUser } from '../utils/session';
import { eventToTimeline } from '../utils/timeline';

const props = defineProps({
  id: { type: String, required: true }
});

const router = useRouter();
const canManage = computed(() => ['ADMIN', 'ENCARGADO'].includes(getUser()?.role));

const detail = ref(null);
const loadError = ref('');
const actionError = ref('');
const actionSuccess = ref('');
const busy = ref(false);

const lots = ref([]);
const products = ref([]);
const newLotId = ref('');
const newQuantity = ref('');
const consumption = ref({});

const order = computed(() => detail.value?.order);
const isOpen = computed(() => order.value?.status === 'ABIERTO');
const timelineItems = computed(() => (detail.value?.events || []).map(eventToTimeline));

const productName = (productId) => products.value.find((product) => product.productId === productId)?.name || '';

// Solo se ofrecen los lotes con stock disponible que aún no están en el pedido
const lotOptions = computed(() => {
  const used = new Set((detail.value?.lines || []).map((line) => line.lotId));
  return lots.value.filter((lot) => lot.availableQuantity > 0 && !used.has(lot.lotId));
});

const load = async () => {
  loadError.value = '';
  try {
    detail.value = await apiClient.get(`/pedidos/${props.id}`);
    // Por defecto, lo consumido es lo reservado
    consumption.value = Object.fromEntries(detail.value.lines.map((line) => [line.lineId, line.reservedQuantity]));
    if (isOpen.value && canManage.value) {
      const [allLots, allProducts] = await Promise.all([apiClient.get('/lotes'), apiClient.get('/productos/admin')]);
      lots.value = allLots;
      products.value = allProducts;
    }
  } catch (error) {
    detail.value = null;
    loadError.value = await errorMessage(error, 'No se pudo cargar el pedido.');
  }
};

const run = async (action, success, fallback) => {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  actionSuccess.value = '';
  try {
    await action();
    actionSuccess.value = success;
    await load();
  } catch (error) {
    actionError.value = await errorMessage(error, fallback);
  } finally {
    busy.value = false;
  }
};

const addLine = () => run(async () => {
  await apiClient.post(`/pedidos/${props.id}/lineas`, {
    lotId: Number(newLotId.value),
    quantity: Number(newQuantity.value)
  });
  newLotId.value = '';
  newQuantity.value = '';
}, 'Cantidad reservada en el lote.', 'No se pudo reservar el lote.');

const removeLine = (line) => {
  if (!confirm(`¿Retirar el lote ${line.lotNumber} del pedido? Se liberará lo reservado.`)) return;
  run(() => apiClient.delete(`/pedidos/${props.id}/lineas/${line.lineId}`), 'Lote retirado del pedido.', 'No se pudo retirar el lote.');
};

const closeOrder = () => {
  if (!confirm('¿Cerrar el pedido? Se restará del stock lo consumido y no se podrá modificar.')) return;
  run(() => apiClient.post(`/pedidos/${props.id}/cerrar`, {
    lines: detail.value.lines.map((line) => ({
      lineId: line.lineId,
      consumedQuantity: Number(consumption.value[line.lineId])
    }))
  }), 'Pedido cerrado y stock actualizado.', 'No se pudo cerrar el pedido.');
};

const cancelOrder = () => {
  if (!confirm('¿Cancelar el pedido? Se liberará todo lo reservado y no se podrá reabrir.')) return;
  run(() => apiClient.post(`/pedidos/${props.id}/cancelar`), 'Pedido cancelado.', 'No se pudo cancelar el pedido.');
};

onMounted(load);
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>{{ order ? `Pedido ${order.orderNumber}` : 'Pedido' }}</h2>
      <button @click="router.push('/pedidos')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p v-if="loadError" class="msg-error">{{ loadError }}</p>

      <template v-if="order">
        <section class="panel">
          <div class="item-head">
            <h3>Datos del pedido</h3>
            <span class="status" :class="`status-${order.status}`">{{ order.status }}</span>
          </div>
          <dl class="facts">
            <div><dt>Cliente</dt><dd>{{ order.customerName || 'Sin cliente' }}</dd></div>
            <div><dt>Fecha programada</dt><dd>{{ formatEpochDate(order.scheduledDate) }}</dd></div>
            <div><dt>Creado por</dt><dd>{{ order.createdBy }}</dd></div>
            <div><dt>Creado el</dt><dd>{{ formatEpochDateTime(order.createdAt) }}</dd></div>
            <div v-if="order.closedAt"><dt>Finalizado por</dt><dd>{{ order.closedBy }}</dd></div>
            <div v-if="order.closedAt"><dt>Finalizado el</dt><dd>{{ formatEpochDateTime(order.closedAt) }}</dd></div>
            <div v-if="order.notes" class="fact-wide"><dt>Observaciones</dt><dd>{{ order.notes }}</dd></div>
          </dl>
        </section>

        <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
        <p v-if="actionError" class="msg-error" role="alert">{{ actionError }}</p>

        <section v-if="isOpen && canManage" class="panel">
          <h3>Asignar lote</h3>
          <form @submit.prevent="addLine" class="line-form">
            <div class="form-group">
              <label for="line-lot">Lote (*):</label>
              <select id="line-lot" v-model="newLotId" required>
                <option value="" disabled>Selecciona un lote...</option>
                <option v-for="lot in lotOptions" :key="lot.lotId" :value="lot.lotId">
                  {{ lot.lotNumber }} · {{ productName(lot.productId) }} · disponible {{ lot.availableQuantity }} {{ lot.measureUnit }}{{ isExpired(lot.expirationDate) ? ' · CADUCADO' : '' }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label for="line-quantity">Cantidad prevista (*):</label>
              <input id="line-quantity" v-model="newQuantity" type="number" min="0.0001" step="0.0001" required />
            </div>
            <button type="submit" :disabled="busy">Reservar</button>
          </form>
        </section>

        <section class="panel">
          <h3>Lotes del pedido</h3>
          <p v-if="detail.lines.length === 0" class="empty-state">Este pedido aún no tiene lotes asignados.</p>
          <div v-else class="item-list">
            <div v-for="line in detail.lines" :key="line.lineId" class="item-card">
              <span class="item-title">{{ line.productName }}</span>
              <RouterLink :to="`/lotes/${line.lotId}/trazabilidad`">{{ line.lotNumber }}</RouterLink>
              <span class="item-meta">Reservado: {{ line.reservedQuantity }} {{ line.measureUnit }}</span>
              <span v-if="line.consumedQuantity !== null" class="item-meta">Consumido: {{ line.consumedQuantity }} {{ line.measureUnit }}</span>
              <div v-if="isOpen" class="form-group line-consumed">
                <label :for="`consumed-${line.lineId}`">Consumo real ({{ line.measureUnit }}):</label>
                <input :id="`consumed-${line.lineId}`" v-model="consumption[line.lineId]" type="number" min="0" step="0.0001" required />
              </div>
              <button v-if="isOpen && canManage" @click="removeLine(line)" :disabled="busy" class="btn-secondary">Retirar del pedido</button>
            </div>
          </div>

          <div v-if="isOpen" class="order-actions">
            <button @click="closeOrder" :disabled="busy || detail.lines.length === 0">Cerrar pedido y restar stock</button>
            <button v-if="canManage" @click="cancelOrder" :disabled="busy" class="btn-danger">Cancelar pedido</button>
          </div>
        </section>

        <section class="panel">
          <h3>Historial del pedido</h3>
          <EventTimeline class="history" :items="timelineItems" />
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 14px;
}

.facts {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  margin: 14px 0 0;
}

.fact-wide {
  grid-column: 1 / -1;
}

dt {
  color: var(--gargantua-gray);
  font-size: 0.825rem;
  font-weight: 600;
}

dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.line-form {
  display: grid;
  align-items: end;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr);
}

@media (min-width: 720px) {
  .line-form {
    grid-template-columns: minmax(0, 3fr) minmax(0, 1fr) auto;
  }
}

.line-consumed {
  margin-top: 6px;
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--gargantua-border);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 480px) {
  .order-actions button {
    width: 100%;
  }
}
</style>
