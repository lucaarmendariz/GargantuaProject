<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { saveSession } from '../utils/session';
import { refreshAccess } from '../utils/access';

const username = ref('');
const password = ref('');
const errorMessage = ref('');
const router = useRouter();

const handleLogin = async () => {
  try {
    errorMessage.value = '';
    
    // Llamada POST nativa a /api/auth/login
    const userData = await apiClient.post('/auth/login', {
      username: username.value,
      password: password.value
    });

    // Guardamos la sesión en el navegador y los permisos de este usuario
    saveSession(userData);
    await refreshAccess({ force: true });

    // Saltamos al panel de control
    router.push('/dashboard');
    
  } catch (error) {
    if (error && error.status === 401) {
      errorMessage.value = 'Usuario o contraseña incorrectos.';
    } else {
      errorMessage.value = 'Error al conectar con el servidor de MonCorp.';
    }
  }
};
</script>

<template>
  <div class="login-container">
    <div class="login-card panel">
      <h2>Gargantua App</h2>
      <p>Control de trazabilidad</p>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>Usuario:</label>
          <input v-model="username" type="text" required placeholder="Ej: luca" />
        </div>
        
        <div class="form-group">
          <label>Contraseña:</label>
          <input v-model="password" type="password" required placeholder="••••••••" />
        </div>
        
        <button type="submit">Entrar al Sistema</button>
      </form>
      
      <p v-if="errorMessage" class="msg-error">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 16px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-top: 4px solid var(--gargantua-gold);
}

h2 {
  text-align: center;
}

.login-card > p:not(.msg-error) {
  margin: 4px 0 24px;
  color: var(--gargantua-gray);
  text-align: center;
}

.form-group {
  margin-bottom: 16px;
}

button {
  width: 100%;
  margin-top: 4px;
}

.msg-error {
  margin-top: 16px;
  text-align: center;
}
</style>
