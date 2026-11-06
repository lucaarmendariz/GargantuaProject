<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { formatEpochDate, formatEpochDateTime } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { matchesSearch } from '../utils/search';

const router = useRouter();

const orders = ref([]);
const loading = ref(true);
const listError = ref('');
const searchQuery = ref('');

const filteredOrders = computed(() => orders.value.filter((order) => matchesSearch(searchQuery.value, [
  order.orderNumber,
  order.customerName,
  order.notes,
  order.createdBy,
  order.preparedBy,
  order.deliveredBy,
  formatEpochDate(order.scheduledDate),
])));

onMounted(async () => {
  try {
    orders.value = await apiClient.get('/pedidos?estado=CERRADO');
  } catch (error) {
    listError.value = await errorMessage(error, 'No se pudieron cargar los pedidos cerrados.');
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Pedidos cerrados</h2>
      <button @click="router.push('/pedidos')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p class="item-meta">
        Pedidos entregados que se cerraron automáticamente a los dos días de la entrega. Solo se pueden consultar.
      </p>

      <div class="search-box">
        <input v-model="searchQuery" type="search" class="search-input" aria-label="Buscar pedidos cerrados"
          placeholder="Buscar por número, cliente, observaciones, operario o fecha (dd/mm/aaaa)..." />
      </div>

      <section class="panel">
        <div class="item-head list-head">
          <h3>Archivo de pedidos</h3>
          <span v-if="!loading && !listError" class="item-meta">
            {{ filteredOrders.length }} de {{ orders.length }} pedido(s)
          </span>
        </div>

        <p v-if="loading" class="empty-state">Cargando pedidos cerrados...</p>
        <p v-else-if="listError" class="msg-error">{{ listError }}</p>
        <p v-else-if="orders.length === 0" class="empty-state">Todavía no hay pedidos cerrados.</p>
        <p v-else-if="filteredOrders.length === 0" class="empty-state">Ningún pedido cerrado coincide con la búsqueda.</p>
        <div v-else class="item-list">
          <RouterLink v-for="order in filteredOrders" :key="order.orderId" :to="`/pedidos/${order.orderId}`" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ order.orderNumber }}</span>
              <span class="status status-CERRADO">CERRADO</span>
            </div>
            <span class="item-meta">{{ order.customerName || 'Sin cliente' }} · {{ order.lineCount }} lote(s)</span>
            <span class="item-meta">Programado: {{ formatEpochDate(order.scheduledDate) }}</span>
            <span v-if="order.deliveredAt" class="item-meta">Entregado: {{ formatEpochDateTime(order.deliveredAt) }} por {{ order.deliveredBy }}</span>
            <span class="item-meta">Cerrado: {{ formatEpochDateTime(order.closedAt) }}</span>
          </RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.list-head {
  margin-bottom: 16px;
}
</style>
