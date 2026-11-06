<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { dateInputToEpoch, epochToDateInput, formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';

const router = useRouter();

const UNITS = [
  { value: 'UNITS', label: 'Unidades (Fichas)' },
  { value: 'KILOS', label: 'Kilogramos (Materia prima)' },
  { value: 'BOXES', label: 'Cajas de empaquetado' },
];

const products = ref([]);
const lots = ref([]);
const loading = ref(true);
const listError = ref('');
const productFilter = ref('');
const lotIdInput = ref('');

const actionError = ref('');
const actionSuccess = ref('');
const busy = ref(false);

// --- ESTADO LOCAL PARA CONTROLAR LA EDICIÓN DE UN LOTE ---
const editingLotId = ref(null);
const editForm = ref({ quantity: '', measureUnit: 'UNITS', expirationDate: '', supplierReference: '' });

const productLabel = (productId) => {
  const product = products.value.find((prod) => prod.productId === productId);
  return product ? `${product.name} (${product.sku})` : `Producto #${productId}`;
};

// Sin filtro se piden todos los lotes; con filtro, solo los del producto elegido
const loadLots = async () => {
  loading.value = true;
  listError.value = '';
  try {
    const endpoint = productFilter.value ? `/lotes/producto/${productFilter.value}` : '/lotes';
    lots.value = await apiClient.get(endpoint);
  } catch (error) {
    lots.value = [];
    listError.value = await errorMessage(error, 'No se pudieron cargar los lotes.');
  } finally {
    loading.value = false;
  }
};

// Búsqueda directa por el ID interno del lote
const searchById = async () => {
  if (!lotIdInput.value) return;
  loading.value = true;
  listError.value = '';
  productFilter.value = '';
  try {
    lots.value = [await apiClient.get(`/lotes/${lotIdInput.value}`)];
  } catch (error) {
    lots.value = [];
    listError.value = error?.status === 404
      ? 'No existe ningún lote con este ID.'
      : await errorMessage(error, 'No se pudo buscar el lote.');
  } finally {
    loading.value = false;
  }
};

const showAll = () => {
  lotIdInput.value = '';
  if (productFilter.value) {
    productFilter.value = '';
  } else {
    loadLots();
  }
};

const startEdit = (lot) => {
  actionError.value = '';
  actionSuccess.value = '';
  editingLotId.value = lot.lotId;
  editForm.value = {
    quantity: lot.quantity,
    measureUnit: lot.measureUnit,
    expirationDate: epochToDateInput(lot.expirationDate),
    supplierReference: lot.supplierReference || '',
  };
};

const cancelEdit = () => {
  editingLotId.value = null;
};

const run = async (action, success, fallback) => {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  actionSuccess.value = '';
  try {
    await action();
    actionSuccess.value = success;
    // Tras un cambio se recarga el listado; la búsqueda por ID deja de aplicarse
    lotIdInput.value = '';
    await loadLots();
  } catch (error) {
    actionError.value = await errorMessage(error, fallback);
  } finally {
    busy.value = false;
  }
};

const saveEdit = (lot) => run(async () => {
  await apiClient.put(`/lotes/${lot.lotId}`, {
    quantity: Number(editForm.value.quantity),
    measureUnit: editForm.value.measureUnit,
    expirationDate: dateInputToEpoch(editForm.value.expirationDate),
    supplierReference: editForm.value.supplierReference || null,
  });
  editingLotId.value = null;
}, `Lote ${lot.lotNumber} actualizado correctamente.`, 'No se pudo actualizar el lote.');

const deleteLot = (lot) => {
  if (!confirm(`¿Eliminar definitivamente el lote ${lot.lotNumber}? Esta acción no se puede deshacer.`)) return;
  run(() => apiClient.delete(`/lotes/${lot.lotId}`), `Lote ${lot.lotNumber} eliminado.`, 'No se pudo eliminar el lote.');
};

onMounted(async () => {
  try {
    products.value = await apiClient.get('/productos/admin');
  } catch (error) {
    console.error('Error al cargar los productos', error);
  }
  await loadLots();
});

watch(productFilter, () => {
  if (productFilter.value) lotIdInput.value = '';
  loadLots();
});
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Gestión de Lotes</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <section class="panel filters">
        <div class="form-group">
          <label for="lot-product-filter">Producto:</label>
          <select id="lot-product-filter" v-model="productFilter">
            <option value="">Todos los productos</option>
            <option v-for="prod in products" :key="prod.productId" :value="prod.productId">
              {{ prod.name }} ({{ prod.sku }})
            </option>
          </select>
        </div>
        <div class="form-group">
          <label for="lot-id-search">Buscar por ID de lote:</label>
          <div class="input-row">
            <input id="lot-id-search" v-model="lotIdInput" type="number" min="1" placeholder="Ej: 1" @keyup.enter="searchById" />
            <button @click="searchById">Buscar</button>
          </div>
        </div>
        <button v-if="productFilter || lotIdInput" @click="showAll" class="btn-secondary show-all">Ver todos</button>
      </section>

      <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
      <p v-if="actionError" class="msg-error" role="alert">{{ actionError }}</p>

      <section class="panel">
        <h3>Lotes registrados</h3>
        <p v-if="loading" class="empty-state">Cargando lotes...</p>
        <p v-else-if="listError" class="msg-error">{{ listError }}</p>
        <p v-else-if="lots.length === 0" class="empty-state">No hay lotes con este filtro.</p>
        <div v-else class="item-list">
          <div v-for="lot in lots" :key="lot.lotId" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ lot.lotNumber }} <code>#{{ lot.lotId }}</code></span>
              <span v-if="isExpired(lot.expirationDate)" class="status status-CADUCADO">CADUCADO</span>
            </div>
            <span class="item-meta">{{ productLabel(lot.productId) }}</span>

            <template v-if="editingLotId !== lot.lotId">
              <div class="stats lot-stats">
                <div class="stat"><span class="stat-label">Entrada</span><span class="stat-value">{{ lot.quantity }} {{ lot.measureUnit }}</span></div>
                <div class="stat"><span class="stat-label">Stock</span><span class="stat-value">{{ lot.currentStock }}</span></div>
                <div class="stat"><span class="stat-label">Reservado</span><span class="stat-value">{{ lot.reservedQuantity }}</span></div>
                <div class="stat"><span class="stat-label">Disponible</span><span class="stat-value">{{ lot.availableQuantity }}</span></div>
              </div>
              <span class="item-meta">
                Caducidad: {{ formatEpochDate(lot.expirationDate) }}
                · Proveedor: {{ lot.supplierReference || 'N/A' }}
              </span>
              <span class="item-meta">Alta: {{ formatEpochDateTime(lot.createdAt) }} · Última modificación: {{ formatEpochDateTime(lot.updatedAt) }}</span>
              <div class="lot-actions">
                <RouterLink :to="`/lotes/${lot.lotId}/trazabilidad`">Ver trazabilidad</RouterLink>
                <button @click="startEdit(lot)" :disabled="busy" class="btn-secondary">Editar</button>
                <button @click="deleteLot(lot)" :disabled="busy" class="btn-danger">Eliminar</button>
              </div>
            </template>

            <form v-else @submit.prevent="saveEdit(lot)" class="edit-form">
              <div class="form-group">
                <label :for="`lot-qty-${lot.lotId}`">Cantidad de entrada (*):</label>
                <input :id="`lot-qty-${lot.lotId}`" v-model="editForm.quantity" type="number" min="0" step="0.0001" required />
              </div>
              <div class="form-group">
                <label :for="`lot-unit-${lot.lotId}`">Unidad medida:</label>
                <select :id="`lot-unit-${lot.lotId}`" v-model="editForm.measureUnit">
                  <option v-for="unit in UNITS" :key="unit.value" :value="unit.value">{{ unit.label }}</option>
                </select>
              </div>
              <div class="form-group">
                <label :for="`lot-exp-${lot.lotId}`">Fecha caducidad:</label>
                <input :id="`lot-exp-${lot.lotId}`" v-model="editForm.expirationDate" type="date" />
              </div>
              <div class="form-group">
                <label :for="`lot-ref-${lot.lotId}`">Referencia albarán / proveedor:</label>
                <input :id="`lot-ref-${lot.lotId}`" v-model="editForm.supplierReference" type="text" />
              </div>
              <p class="item-meta form-wide">
                La cantidad no puede ser menor que lo ya reservado ({{ lot.reservedQuantity }}) más lo consumido ({{ lot.consumedQuantity }}).
              </p>
              <div class="lot-actions form-wide">
                <button type="submit" :disabled="busy">Guardar</button>
                <button type="button" @click="cancelEdit" class="btn-secondary">Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 14px;
}

.filters {
  display: grid;
  align-items: end;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
}

.show-all {
  justify-self: start;
}

.lot-stats {
  margin: 8px 0;
}

.lot-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.edit-form {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  margin-top: 8px;
}

.form-wide {
  grid-column: 1 / -1;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 480px) {
  .lot-actions button {
    width: 100%;
  }
}
</style>
