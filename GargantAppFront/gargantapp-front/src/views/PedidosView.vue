<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import TabMenu from '../components/TabMenu.vue';
import PedidosCerradosList from '../components/PedidosCerradosList.vue';
import { useTabRoute } from '../composables/useTabRoute';
import { dateInputToEpoch, formatEpochDate, todayInputValue } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { getUser } from '../utils/session';

const router = useRouter();
const canManage = computed(() => ['ADMIN', 'ENCARGADO'].includes(getUser()?.role));

// Menú del módulo: una acción por pestaña. Crear pedidos solo para ADMIN y ENCARGADO.
const tabs = computed(() => [
  { id: 'en-curso', label: 'Pedidos en curso' },
  ...(canManage.value ? [{ id: 'nuevo', label: 'Nuevo pedido' }] : []),
  { id: 'cerrados', label: 'Pedidos cerrados' },
]);
const activeTab = useTabRoute(tabs);

const orders = ref([]);
const loading = ref(true);
const listError = ref('');
const IN_PROGRESS = 'ABIERTO,PREPARADO,ENTREGADO';
const statusFilter = ref(IN_PROGRESS);

const orderNumber = ref('');
const customerName = ref('');
const scheduledDate = ref(todayInputValue());
const notes = ref('');
const formError = ref('');

const loadOrders = async () => {
  loading.value = true;
  listError.value = '';
  try {
    // Los pedidos cerrados tienen su propia pestaña
    orders.value = await apiClient.get(`/pedidos?estado=${statusFilter.value}`);
  } catch (error) {
    orders.value = [];
    listError.value = await errorMessage(error, 'No se pudieron cargar los pedidos.');
  } finally {
    loading.value = false;
  }
};

const createOrder = async () => {
  formError.value = '';
  try {
    const orderId = await apiClient.post('/pedidos', {
      orderNumber: orderNumber.value,
      customerName: customerName.value || null,
      scheduledDate: dateInputToEpoch(scheduledDate.value),
      notes: notes.value || null
    });
    router.push(`/pedidos/${orderId}`);
  } catch (error) {
    formError.value = await errorMessage(error, 'No se pudo crear el pedido.');
  }
};

onMounted(loadOrders);
watch(statusFilter, loadOrders);
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Pedidos</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <TabMenu v-model="activeTab" :tabs="tabs" label="Acciones de pedidos" />

      <p v-if="activeTab !== 'cerrados'" class="item-meta flow-help">
        Ciclo de un pedido: Abierto → Preparado → Entregado → Cerrado (automático a los 2 días de la entrega).
      </p>

      <section v-if="activeTab === 'nuevo'" id="panel-nuevo" role="tabpanel" aria-labelledby="tab-nuevo" class="panel">
        <h3>Nuevo pedido</h3>
        <form @submit.prevent="createOrder" class="order-form">
          <div class="form-group">
            <label for="order-number">Número de pedido (*):</label>
            <input id="order-number" v-model="orderNumber" type="text" required placeholder="Ej: PED-2026-001" />
          </div>
          <div class="form-group">
            <label for="order-customer">Cliente:</label>
            <input id="order-customer" v-model="customerName" type="text" placeholder="Ej: Bar Central" />
          </div>
          <div class="form-group">
            <label for="order-date">Fecha programada (*):</label>
            <input id="order-date" v-model="scheduledDate" type="date" required />
          </div>
          <div class="form-group form-wide">
            <label for="order-notes">Observaciones:</label>
            <textarea id="order-notes" v-model="notes" rows="2"></textarea>
          </div>
          <div class="form-wide">
            <button type="submit">Crear pedido</button>
          </div>
          <p v-if="formError" class="msg-error form-wide">{{ formError }}</p>
        </form>
      </section>

      <section v-if="activeTab === 'en-curso'" id="panel-en-curso" role="tabpanel" aria-labelledby="tab-en-curso" class="panel">
        <div class="list-head">
          <h3>Listado de pedidos</h3>
          <div class="form-group filter">
            <label for="order-filter">Estado:</label>
            <select id="order-filter" v-model="statusFilter">
              <option :value="IN_PROGRESS">En curso</option>
              <option value="ABIERTO">Abiertos</option>
              <option value="PREPARADO">Preparados</option>
              <option value="ENTREGADO">Entregados</option>
              <option value="CANCELADO">Cancelados</option>
            </select>
          </div>
        </div>

        <p v-if="loading" class="empty-state">Cargando pedidos...</p>
        <p v-else-if="listError" class="msg-error">{{ listError }}</p>
        <p v-else-if="orders.length === 0" class="empty-state">No hay pedidos con este filtro.</p>
        <div v-else class="item-list">
          <RouterLink v-for="order in orders" :key="order.orderId" :to="`/pedidos/${order.orderId}`" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ order.orderNumber }}</span>
              <span class="status" :class="`status-${order.status}`">{{ order.status }}</span>
            </div>
            <span class="item-meta">{{ order.customerName || 'Sin cliente' }}</span>
            <span class="item-meta">{{ formatEpochDate(order.scheduledDate) }} · {{ order.lineCount }} lote(s)</span>
          </RouterLink>
        </div>
      </section>

      <div v-if="activeTab === 'cerrados'" id="panel-cerrados" role="tabpanel" aria-labelledby="tab-cerrados" class="tab-panel">
        <PedidosCerradosList />
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 16px;
}

.order-form {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
}

.form-wide {
  grid-column: 1 / -1;
}

.list-head {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.filter {
  flex: 0 1 220px;
}</style>
