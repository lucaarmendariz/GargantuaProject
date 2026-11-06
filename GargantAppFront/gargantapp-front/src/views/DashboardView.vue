<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { clearSession } from '../utils/session';
import { dayRange, formatDayLong, formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';

const user = ref(null);
const lotInput = ref('');
const lotData = ref(null);
const errorMsg = ref('');
const router = useRouter();

// --- NUEVO ESTADO PARA EL CALENDARIO INTERACTIVO ---
const currentDate = ref(new Date());
const selectedDate = ref(new Date());
const weekDays = ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá'];
const months = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
];

onMounted(() => {
  // Verificar sesión
  const savedUser = localStorage.getItem('user');
  if (!savedUser) {
    router.push('/');
  } else {
    user.value = JSON.parse(savedUser);
    loadDay();
  }
});

// --- LÓGICA DEL MOTOR DEL CALENDARIO ---
const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonthName = computed(() => months[currentDate.value.getMonth()]);

// Genera la matriz de días que se pintará en el grid del calendario
const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear();
  const month = currentDate.value.getMonth();

  // Primer día del mes actual y cuántos días tiene
  const firstDayIndex = new Date(year, month, 1).getDay();
  const totalDays = new Date(year, month + 1, 0).getDate();

  // Días del mes anterior (para rellenar los huecos iniciales de la cuadrícula)
  const prevTotalDays = new Date(year, month, 0).getDate();

  const daysArray = [];

  // Huecos del mes anterior
  for (let i = firstDayIndex - 1; i >= 0; i--) {
    daysArray.push({
      day: prevTotalDays - i,
      isCurrentMonth: false,
      date: new Date(year, month - 1, prevTotalDays - i)
    });
  }

  // Días del mes actual
  for (let i = 1; i <= totalDays; i++) {
    daysArray.push({
      day: i,
      isCurrentMonth: true,
      date: new Date(year, month, i)
    });
  }

  // Huecos del mes siguiente para completar la última semana completa si es necesario
  const remainingCells = 42 - daysArray.length; // Estándar de 6 filas (7x6 = 42)
  for (let i = 1; i <= remainingCells; i++) {
    daysArray.push({
      day: i,
      isCurrentMonth: false,
      date: new Date(year, month + 1, i)
    });
  }

  return daysArray;
});

// Navegación del calendario
const prevMonth = () => {
  currentDate.value = new Date(currentYear.value, currentDate.value.getMonth() - 1, 1);
};

const nextMonth = () => {
  currentDate.value = new Date(currentYear.value, currentDate.value.getMonth() + 1, 1);
};

const selectDay = (dayObj) => {
  selectedDate.value = dayObj.date;
  // Si seleccionas un día del mes anterior o siguiente, movemos el calendario a ese mes
  if (!dayObj.isCurrentMonth) {
    currentDate.value = new Date(dayObj.date.getFullYear(), dayObj.date.getMonth(), 1);
  }
};

// Comprobar si un día de la cuadrícula es "Hoy"
const isToday = (date) => {
  const today = new Date();
  return date.getDate() === today.getDate() &&
         date.getMonth() === today.getMonth() &&
         date.getFullYear() === today.getFullYear();
};

// Comprobar si un día es el seleccionado por el operario
const isSelected = (date) => {
  return date.getDate() === selectedDate.value.getDate() &&
         date.getMonth() === selectedDate.value.getMonth() &&
         date.getFullYear() === selectedDate.value.getFullYear();
};

// --- PEDIDOS Y PRODUCTOS EN USO DEL DIA SELECCIONADO ---
const dayOrders = ref([]);
const inUseLines = ref([]);
const dayLoading = ref(true);
const dayError = ref('');
const selectedDayLabel = computed(() => formatDayLong(selectedDate.value));
let dayRequest = 0;

const loadDay = async () => {
  // Si se pulsan varios días seguidos, solo cuenta la última respuesta
  const request = ++dayRequest;
  dayLoading.value = true;
  dayError.value = '';
  const { from, to } = dayRange(selectedDate.value);
  try {
    const [orders, lines] = await Promise.all([
      apiClient.get(`/pedidos?desde=${from}&hasta=${to}`),
      apiClient.get(`/pedidos/en-uso?desde=${from}&hasta=${to}`)
    ]);
    if (request !== dayRequest) return;
    dayOrders.value = orders;
    inUseLines.value = lines;
  } catch (error) {
    const message = await errorMessage(error, 'No se pudieron cargar los pedidos del día.');
    if (request !== dayRequest) return;
    dayOrders.value = [];
    inUseLines.value = [];
    dayError.value = message;
  } finally {
    if (request === dayRequest) dayLoading.value = false;
  }
};

watch(selectedDate, loadDay);

// --- RESTO DE FUNCIONES ---
const navigateTo = (path) => {
  router.push(path);
};

const searchLot = async () => {
  try {
    errorMsg.value = '';
    lotData.value = null;
    lotData.value = await apiClient.get(`/trazabilidad/lotes/${lotInput.value}/status`);
  } catch (err) {
    errorMsg.value = err?.status === 404
      ? 'El lote escaneado no existe o no tiene historial registrado.'
      : 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
  }
};

const logout = () => {
  clearSession();
  router.push('/');
};
</script>

<template>
  <div v-if="user" class="content">
    <header class="card-header">
      <div class="brand-title">
        <h1>Panel Central</h1>
        <p>Planta Gargantúa Tortillas</p>
      </div>
      <div class="user-badge">
        <strong>{{ user.name }}</strong>
        <button @click="logout" class="btn-logout">Salir</button>
      </div>
    </header>

    <main class="card-body">

      <section class="control-panel">

        <div class="full-calendar-widget panel">
          <div class="cal-nav">
            <button @click="prevMonth" class="cal-nav-btn">Anterior</button>
            <span class="cal-title"><strong>{{ currentMonthName }}</strong> {{ currentYear }}</span>
            <button @click="nextMonth" class="cal-nav-btn">Siguiente</button>
          </div>
          
          <div class="cal-weekdays">
            <span v-for="day in weekDays" :key="day" class="weekday-cell">{{ day }}</span>
          </div>
          
          <div class="cal-days-grid">
            <button 
              v-for="(dayObj, idx) in calendarDays" 
              :key="idx"
              @click="selectDay(dayObj)"
              class="day-cell"
              :class="{
                'outside-month': !dayObj.isCurrentMonth,
                'today-cell': isToday(dayObj.date),
                'selected-cell': isSelected(dayObj.date)
              }"
            >
              {{ dayObj.day }}
            </button>
          </div>
        </div>

        <div class="operations-menu">
          <button @click="navigateTo('/pedidos')" class="menu-btn btn-pedidos">
            <span class="text">Pedidos</span>
          </button>

          <button @click="navigateTo('/inventario')" class="menu-btn btn-inventario">
            <span class="text">Inventario Stock</span>
          </button>

          <button @click="navigateTo('/lotes')" class="menu-btn btn-lotes">
            <span class="text">Gestión de Lotes</span>
          </button>

          <button @click="navigateTo('/historial-global')" class="menu-btn btn-historial">
            <span class="text">Historial Visibilizar</span>
          </button>

          <button @click="navigateTo('/produccion')" class="menu-btn btn-produccion">
            <span class="text">Producción Planta</span>
          </button>
        </div>

      </section>

      <section class="panel day-section">
        <h3>Pedidos del {{ selectedDayLabel }}</h3>
        <p v-if="dayLoading" class="empty-state">Cargando pedidos...</p>
        <p v-else-if="dayError" class="msg-error">{{ dayError }}</p>
        <p v-else-if="dayOrders.length === 0" class="empty-state">No hay pedidos programados este día.</p>
        <div v-else class="item-list">
          <RouterLink v-for="order in dayOrders" :key="order.orderId" :to="`/pedidos/${order.orderId}`" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ order.orderNumber }}</span>
              <span class="status" :class="`status-${order.status}`">{{ order.status }}</span>
            </div>
            <span class="item-meta">{{ order.customerName || 'Sin cliente' }} · {{ order.lineCount }} lote(s)</span>
          </RouterLink>
        </div>
      </section>

      <section v-if="!dayError" class="panel day-section">
        <h3>Productos en uso</h3>
        <p class="item-meta section-help">Lotes reservados en los pedidos abiertos de este día.</p>
        <p v-if="dayLoading" class="empty-state">Cargando productos...</p>
        <p v-else-if="inUseLines.length === 0" class="empty-state">No hay productos en uso este día.</p>
        <div v-else class="item-list">
          <div v-for="line in inUseLines" :key="line.lineId" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ line.productName }}</span>
              <span v-if="isExpired(line.expirationDate)" class="status status-CADUCADO">CADUCADO</span>
            </div>
            <RouterLink :to="`/lotes/${line.lotId}/trazabilidad`">Lote {{ line.lotNumber }}</RouterLink>
            <RouterLink :to="`/pedidos/${line.orderId}`">Pedido {{ line.orderNumber }}</RouterLink>
            <span class="item-meta">En uso: {{ line.reservedQuantity }} {{ line.measureUnit }}</span>
            <span class="item-meta">Stock del lote: {{ line.currentStock }} · Disponible: {{ line.availableQuantity }}</span>
            <span class="item-meta">Reservado: {{ formatEpochDateTime(line.reservedAt) }} · Caducidad: {{ formatEpochDate(line.expirationDate) }}</span>
          </div>
        </div>
      </section>

      <section class="search-section panel">
        <h3>Simulador de Cámara OCR de Planta</h3>
        <div class="input-row">
          <input v-model="lotInput" placeholder="Introduce o escanea un número de lote..." @keyup.enter="searchLot" />
          <button @click="searchLot">Escanear Lote</button>
        </div>
        <p v-if="errorMsg" class="msg-error">{{ errorMsg }}</p>
      </section>

      <section v-if="lotData" class="result-card panel">
        <h3>Información de Trazabilidad del Lote</h3>
        <div class="table-responsive">
          <table class="info-table">
            <tbody>
              <tr>
                <th>Artículo Maestro:</th>
                <td>{{ lotData.productName }} (SKU: {{ lotData.productSku }})</td>
              </tr>
              <tr>
                <th>Código de Lote:</th>
                <td><strong>{{ lotData.lotNumber }}</strong></td>
              </tr>
              <tr>
                <th>Cantidad Inicial:</th>
                <td>{{ lotData.initialQuantity }} {{ lotData.measureUnit }}</td>
              </tr>
              <tr>
                <th>Stock Actual:</th>
                <td>{{ lotData.currentStock }} {{ lotData.measureUnit }}</td>
              </tr>
              <tr>
                <th>Disponible:</th>
                <td>{{ lotData.availableQuantity }} {{ lotData.measureUnit }} (reservado: {{ lotData.reservedQuantity }})</td>
              </tr>
              <tr>
                <th>Estado Actual:</th>
                <td><span class="status-badge">{{ lotData.currentState || 'Sin movimientos' }}</span></td>
              </tr>
              <tr>
                <th>Última Actualización:</th>
                <td>{{ formatEpochDateTime(lotData.lastUpdate) }}</td>
              </tr>
              <tr>
                <th>Último Operario:</th>
                <td>{{ lotData.lastOperator || 'N/A' }}</td>
              </tr>
              <tr>
                <th>Lectura OCR en Bruto:</th>
                <td><code v-if="lotData.ocrRawText">{{ lotData.ocrRawText }}</code><template v-else>N/A</template></td>
              </tr>
              <tr>
                <th>Observaciones de Calidad:</th>
                <td>{{ lotData.lastObservations || 'N/A' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <RouterLink class="trace-link" :to="`/lotes/${lotData.lotId}/trazabilidad`">Ver trazabilidad completa del lote</RouterLink>
      </section>

    </main>
  </div>
</template>

<style scoped>
.brand-title p {
  color: var(--gargantua-gold);
  font-size: 0.85rem;
  font-weight: 600;
}

.user-badge {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  min-width: 0;
}

.btn-logout {
  background: transparent;
  border-color: var(--gargantua-gold);
  color: var(--gargantua-cream);
}

.btn-logout:hover {
  background: var(--gargantua-gold);
  color: var(--gargantua-dark);
}

.control-panel {
  display: grid;
  gap: clamp(16px, 3vw, 24px);
  grid-template-columns: minmax(0, 1fr);
}

@media (min-width: 860px) {
  .control-panel {
    grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  }
}

/* Calendario */
.full-calendar-widget {
  user-select: none;
}

.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.cal-nav-btn {
  padding: 4px 10px;
  background: var(--gargantua-cream);
  border-color: var(--gargantua-border);
  color: var(--gargantua-dark);
  font-size: 0.85rem;
}

.cal-nav-btn:hover {
  background: var(--gargantua-gold);
}

.cal-title {
  color: var(--gargantua-dark);
  text-align: center;
}

/* En las pantallas mas pequenas el mes pasa encima de los botones */
@media (max-width: 340px) {
  .cal-nav {
    flex-wrap: wrap;
  }

  .cal-title {
    flex: 1 1 100%;
    order: -1;
  }
}

.cal-weekdays,
.cal-days-grid {
  display: grid;
  gap: 4px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.cal-weekdays {
  margin-bottom: 6px;
  color: var(--gargantua-orange);
  font-size: 0.8rem;
  font-weight: 700;
  text-align: center;
}

.day-cell {
  min-height: 40px;
  padding: 0;
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
  font-size: 0.875rem;
}

.day-cell:hover {
  background: var(--gargantua-gold);
}

.outside-month {
  background: transparent;
  color: #8a7d6e;
  font-weight: 400;
}

.today-cell {
  border: 2px solid var(--gargantua-orange);
}

.selected-cell,
.selected-cell:hover {
  background: var(--gargantua-dark);
  color: var(--gargantua-gold);
}

/* Accesos a las secciones */
.operations-menu {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 180px), 1fr));
}

.menu-btn {
  min-height: 72px;
  padding: 16px;
  background: var(--gargantua-white);
  border-color: var(--gargantua-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  color: var(--gargantua-dark);
}

.menu-btn:hover {
  background: var(--gargantua-white);
  border-color: var(--gargantua-orange);
  color: var(--gargantua-orange-dark);
}

.day-section h3 {
  margin-bottom: 12px;
}

.day-section h3::first-letter {
  text-transform: uppercase;
}

.section-help {
  margin-top: -8px;
  margin-bottom: 12px;
}

.trace-link {
  display: inline-block;
  margin-top: 14px;
}

.search-section h3,
.result-card h3 {
  margin-bottom: 12px;
}

.msg-error {
  margin-top: 12px;
}

/* Datos del lote */
.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table th,
.info-table td {
  padding: 12px;
  border-bottom: 1px solid var(--gargantua-border);
  font-size: 0.95rem;
  overflow-wrap: anywhere;
  text-align: left;
}

.info-table tr:last-child th,
.info-table tr:last-child td {
  border-bottom: 0;
}

.info-table th {
  width: 30%;
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
  font-weight: 600;
}

.status-badge {
  padding: 3px 10px;
  background: var(--gargantua-gold);
  border-radius: var(--radius-sm);
  color: var(--gargantua-dark);
  font-size: 0.8rem;
  font-weight: 700;
}

.info-table code {
  color: var(--gargantua-orange-dark);
}

/* En el movil, cada dato ocupa su propia linea */
@media (max-width: 560px) {
  .info-table,
  .info-table tbody,
  .info-table tr,
  .info-table th,
  .info-table td {
    display: block;
    width: 100%;
  }

  .info-table tr {
    padding: 10px 0;
    border-bottom: 1px solid var(--gargantua-border);
  }

  .info-table tr:last-child {
    border-bottom: 0;
  }

  .info-table th,
  .info-table td {
    padding: 0;
    border-bottom: 0;
  }

  .info-table th {
    background: transparent;
    color: var(--gargantua-gray);
    font-size: 0.85rem;
  }
}
</style>
