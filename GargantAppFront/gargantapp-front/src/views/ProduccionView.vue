<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import CameraScanner from '../components/CameraScanner.vue';
import { parseLabel } from '../utils/labelParser';

const router = useRouter();

// Cámara: lee la etiqueta, busca el lote por su número y rellena el ID y el texto OCR
const scannerOpen = ref(false);
const scannedLot = ref('');
const onScan = async ({ text, source }) => {
  scannerOpen.value = false;
  successMessage.value = '';
  errorMessage.value = '';
  scannedLot.value = '';
  const label = parseLabel(text, { source });
  // El campo es de una línea: se separan las líneas de la etiqueta con " | "
  ocrRawText.value = label.raw.split(/\s*\n\s*/).filter(Boolean).join(' | ');
  if (!label.lotNumber) {
    errorMessage.value = 'No se ha encontrado un número de lote en lo leído. Indica el ID del lote a mano.';
    return;
  }
  try {
    const status = await apiClient.get(`/trazabilidad/lotes/${encodeURIComponent(label.lotNumber)}/status`);
    lotId.value = status.lotId;
    scannedLot.value = `${status.lotNumber} · ${status.productName} (estado: ${status.currentState || 'sin movimientos'})`;
  } catch (error) {
    errorMessage.value = error?.status === 404
      ? `El lote ${label.lotNumber} no existe en el sistema.`
      : 'No se pudo consultar el lote. Inténtalo de nuevo.';
  }
};
const lotId = ref('');
const actionType = ref('EN_PRODUCCION');
const quantityAffected = ref('');
const ocrRawText = ref('');
const observations = ref('');

const successMessage = ref('');
const errorMessage = ref('');
const operatorName = ref('Operario Anónimo');

onMounted(() => {
  const savedUser = localStorage.getItem('user');
  if (savedUser) {
    operatorName.value = JSON.parse(savedUser).name;
  }
});

const submitProductionLog = async () => {
  try {
    successMessage.value = '';
    errorMessage.value = '';

    // POST nativo a /api/historial/escanear
    const responseMessage = await apiClient.post('/historial/escanear', {
      lotId: parseInt(lotId.value),
      actionType: actionType.value,
      quantityAffected: quantityAffected.value ? parseFloat(quantityAffected.value) : null,
      ocrRawText: ocrRawText.value || `MANUAL_ENTRY_PROD_FLOOR_${Date.now()}`,
      observations: observations.value
    });

    successMessage.value = 'Movimiento de producción enviado y guardado correctamente.';
    
    // Limpiar el formulario menos el operario
    lotId.value = ''; quantityAffected.value = ''; ocrRawText.value = ''; observations.value = ''; scannedLot.value = '';
  } catch (error) {
    errorMessage.value = 'Error al registrar el movimiento. Verifica que el ID de lote sea válido.';
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Terminal de Producción en Planta</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p class="subtitle">Registrar manualmente un cambio de estado en la cadena de elaboración.</p>
      
      <form @submit.prevent="submitProductionLog" class="prod-form panel">
        <div class="scan-row">
          <button type="button" class="btn-secondary" @click="scannerOpen = true">Escanear etiqueta con la cámara</button>
          <span v-if="scannedLot" class="item-meta" role="status">Lote leído: {{ scannedLot }}</span>
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label>ID Interno del Lote (*):</label>
            <input v-model="lotId" type="number" required placeholder="Ej: 1" />
          </div>

          <div class="form-group">
            <label>Operación / Estado:</label>
            <select v-model="actionType">
              <option value="EN_PRODUCCION">EN PRODUCCIÓN (Cocinado/Giro)</option>
              <option value="BLOQUEADO">BLOQUEADO (Control de Calidad)</option>
              <option value="VENDIDO">VENDIDO (Salida de Almacén)</option>
            </select>
          </div>

          <div class="form-group">
            <label>Cantidad Afectada:</label>
            <input v-model="quantityAffected" type="number" step="0.0001" placeholder="Ej: 50.00" />
          </div>

          <div class="form-group">
            <label>Texto del Escáner OCR (Opcional):</label>
            <input v-model="ocrRawText" type="text" placeholder="Lectura en bruto de la cámara..." />
          </div>
        </div>

        <div class="form-group full-width">
          <label>Observaciones de Planta:</label>
          <textarea v-model="observations" rows="3" placeholder="Detalles de la cochura o lote de huevos/patatas usado..."></textarea>
        </div>

        <div class="footer-actions">
          <span class="operator-tag">Registra: {{ operatorName }}</span>
          <button type="submit" class="btn-submit">Enviar a Base de Datos</button>
        </div>
      </form>

      <p v-if="successMessage" class="msg-success">{{ successMessage }}</p>
      <p v-if="errorMessage" class="msg-error">{{ errorMessage }}</p>
    </div>

    <CameraScanner v-if="scannerOpen" title="Escanear etiqueta del lote" @result="onScan" @close="scannerOpen = false" />
  </div>
</template>

<style scoped>
.scan-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
}

.subtitle {
  color: var(--gargantua-gray);
}

.prod-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
}

.footer-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--gargantua-border);
}

.operator-tag {
  color: var(--gargantua-gray);
  font-size: 0.9rem;
  font-weight: 600;
}

@media (max-width: 480px) {
  .btn-submit {
    width: 100%;
  }
}
</style>
