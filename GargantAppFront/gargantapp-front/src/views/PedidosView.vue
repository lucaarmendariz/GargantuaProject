<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { dateInputToEpoch, formatEpochDate, todayInputValue } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { getUser } from '../utils/session';

const router = useRouter();
const canManage = computed(() => ['ADMIN', 'ENCARGADO'].includes(getUser()?.role));

const orders = ref([]);
const loading = ref(true);
const listError = ref('');
const statusFilter = ref('');

const orderNumber = ref('');
const customerName = ref('');
const scheduledDate = ref(todayInputValue());
const notes = ref('');
const formError = ref('');

const loadOrders = async () => {
  loading.value = true;
  listError.value = '';
  try {
    const query = statusFilter.value ? `?estado=${statusFilter.value}` : '';
    orders.value = await apiClient.get(`/pedidos${query}`);
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
      <section v-if="canManage" class="panel">
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

      <section class="panel">
        <div class="list-head">
          <h3>Listado de pedidos</h3>
          <div class="form-group filter">
            <label for="order-filter">Estado:</label>
            <select id="order-filter" v-model="statusFilter">
              <option value="">Todos</option>
              <option value="ABIERTO">Abiertos</option>
              <option value="CERRADO">Cerrados</option>
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
}
</style>
