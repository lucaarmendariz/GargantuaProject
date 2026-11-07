<script setup>
import { ref } from 'vue';
import apiClient from '../api';
import { errorMessage } from '../utils/errors';

const emit = defineEmits(['success']);

const prodSku = ref('');
const prodGtin = ref('');
const prodName = ref('');
const prodDesc = ref('');
const prodSuccess = ref('');
const prodError = ref('');

const handleCreateProduct = async () => {
  try {
    prodSuccess.value = '';
    prodError.value = '';

    await apiClient.post('/productos', {
      sku: prodSku.value,
      gtin: prodGtin.value || null,
      name: prodName.value,
      description: prodDesc.value
    });

    prodSuccess.value = 'Producto maestro añadido al catálogo.';
    prodSku.value = ''; prodGtin.value = ''; prodName.value = ''; prodDesc.value = '';
    
    emit('success');
  } catch (error) {
    prodError.value = await errorMessage(error, 'Error al crear el producto. Comprueba que el SKU no esté duplicado.');
  }
};
</script>

<template>
  <section class="form-section panel">
    <h3>Añadir definición de activo (producto maestro)</h3>
    <form @submit.prevent="handleCreateProduct" class="inner-form">
      <div class="form-grid-inputs">
        <div class="form-group">
          <label for="prod-sku">Código SKU (*):</label>
          <input id="prod-sku" v-model="prodSku" type="text" required placeholder="Ej: GAR-TOR-01" />
        </div>
        <div class="form-group">
          <label for="prod-gtin">Código de barras (EAN/GTIN):</label>
          <input id="prod-gtin" v-model="prodGtin" type="text" inputmode="numeric" pattern="[0-9 \-]{8,17}"
            title="8, 12, 13 o 14 cifras" placeholder="Ej: 8412345678905" />
        </div>
      </div>
      <div class="form-group">
        <label for="prod-name">Nombre Comercial (*):</label>
        <input id="prod-name" v-model="prodName" type="text" required placeholder="Ej: Tortilla con Cebolla" />
      </div>
      <div class="form-group">
        <label for="prod-desc">Descripción Técnica:</label>
        <textarea id="prod-desc" v-model="prodDesc" rows="2" placeholder="Ingredientes, alérgenos..."></textarea>
      </div>
      <button type="submit" class="btn-submit btn-prod">Añadir al Catálogo</button>
      <p v-if="prodSuccess" class="msg-success">{{ prodSuccess }}</p>
      <p v-if="prodError" class="msg-error">{{ prodError }}</p>
    </form>
  </section>
</template>
