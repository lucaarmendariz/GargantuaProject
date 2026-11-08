<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { errorMessage } from '../utils/errors';

// Cada usuario cambia su propia contraseña; tiene que saber la actual.
const router = useRouter();
const current = ref('');
const next = ref('');
const repeat = ref('');
const success = ref('');
const error = ref('');
const busy = ref(false);

const changePassword = async () => {
  success.value = '';
  error.value = '';
  if (next.value !== repeat.value) {
    error.value = 'La contraseña nueva y su repetición no coinciden.';
    return;
  }
  busy.value = true;
  try {
    await apiClient.put('/me/password', { actual: current.value, nueva: next.value });
    success.value = 'Contraseña cambiada. Úsala la próxima vez que entres.';
    current.value = ''; next.value = ''; repeat.value = '';
  } catch (e) {
    error.value = await errorMessage(e, 'No se pudo cambiar la contraseña.');
  } finally {
    busy.value = false;
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Mi contraseña</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <section class="panel form-tab">
        <form @submit.prevent="changePassword" class="password-form">
          <div class="form-group">
            <label for="pw-current">Contraseña actual:</label>
            <input id="pw-current" v-model="current" type="password" required autocomplete="current-password" />
          </div>
          <div class="form-group">
            <label for="pw-new">Contraseña nueva (8 a 72 caracteres):</label>
            <input id="pw-new" v-model="next" type="password" required minlength="8" maxlength="72" autocomplete="new-password" />
          </div>
          <div class="form-group">
            <label for="pw-repeat">Repite la contraseña nueva:</label>
            <input id="pw-repeat" v-model="repeat" type="password" required minlength="8" maxlength="72" autocomplete="new-password" />
          </div>
          <button type="submit" :disabled="busy">Cambiar contraseña</button>
          <p v-if="success" class="msg-success" role="status">{{ success }}</p>
          <p v-if="error" class="msg-error" role="alert">{{ error }}</p>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.form-tab {
  max-width: 560px;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
</style>
