<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import TabMenu from '../components/TabMenu.vue';
import UserAvatar from '../components/UserAvatar.vue';
import { useTabRoute } from '../composables/useTabRoute';
import { setAvatar } from '../utils/avatars';
import { formatEpochDate } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { resizeToSquare } from '../utils/image';
import { getUser, saveSession } from '../utils/session';

// Mi perfil: cada usuario ve sus datos y cambia su nombre, su foto y su contraseña
const router = useRouter();
const tabs = [
  { id: 'datos', label: 'Mis datos' },
  { id: 'clave', label: 'Contraseña' },
];
const activeTab = useTabRoute(tabs);

const ROLE_NAMES = {
  SUPERADMIN: 'Proveedor de la aplicación',
  ADMIN: 'Administrador',
  ENCARGADO: 'Encargado',
  OPERARIO: 'Operario',
};

// --- Mis datos ---
const profile = ref(null);
const name = ref('');
const avatar = ref(null);
const dataSuccess = ref('');
const dataError = ref('');
const savingData = ref(false);
const photoInput = ref(null);

onMounted(async () => {
  try {
    profile.value = await apiClient.get('/me/perfil');
    name.value = profile.value.name;
    avatar.value = profile.value.avatar;
  } catch (e) {
    dataError.value = await errorMessage(e, 'No se pudo cargar tu perfil.');
  }
});

const onPhoto = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  dataError.value = '';
  try {
    avatar.value = await resizeToSquare(file);
  } catch {
    dataError.value = 'No se pudo leer la imagen. Prueba con una foto JPEG o PNG.';
  }
};

const saveData = async () => {
  dataSuccess.value = '';
  dataError.value = '';
  savingData.value = true;
  try {
    const { perfil, token } = await apiClient.put('/me/perfil', { name: name.value, avatar: avatar.value });
    profile.value = perfil;
    name.value = perfil.name;
    // El token nuevo lleva el nombre nuevo, que es el que sale en la cabecera
    saveSession({ ...getUser(), name: perfil.name, token });
    setAvatar(perfil.username, perfil.avatar);
    dataSuccess.value = 'Datos guardados.';
  } catch (e) {
    dataError.value = await errorMessage(e, 'No se pudieron guardar tus datos.');
  } finally {
    savingData.value = false;
  }
};

// --- Contraseña ---
const current = ref('');
const next = ref('');
const repeat = ref('');
const passwordSuccess = ref('');
const passwordError = ref('');
const savingPassword = ref(false);

const changePassword = async () => {
  passwordSuccess.value = '';
  passwordError.value = '';
  if (next.value !== repeat.value) {
    passwordError.value = 'La contraseña nueva y su repetición no coinciden.';
    return;
  }
  savingPassword.value = true;
  try {
    await apiClient.put('/me/password', { actual: current.value, nueva: next.value });
    passwordSuccess.value = 'Contraseña cambiada. Úsala la próxima vez que entres.';
    current.value = ''; next.value = ''; repeat.value = '';
  } catch (e) {
    passwordError.value = await errorMessage(e, 'No se pudo cambiar la contraseña.');
  } finally {
    savingPassword.value = false;
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Mi perfil</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <TabMenu v-model="activeTab" :tabs="tabs" label="Mi perfil" />

      <section v-if="activeTab === 'datos'" id="panel-datos" role="tabpanel" aria-labelledby="tab-datos" class="panel form-tab">
        <form v-if="profile" @submit.prevent="saveData" class="profile-form">
          <div class="photo-row">
            <UserAvatar :src="avatar" :name="name" :size="96" />
            <div class="photo-actions">
              <button type="button" class="btn-secondary" @click="photoInput.click()">
                {{ avatar ? 'Cambiar foto' : 'Subir foto' }}
              </button>
              <button v-if="avatar" type="button" class="btn-secondary" @click="avatar = null">Quitar foto</button>
              <input ref="photoInput" type="file" accept="image/*" class="photo-input" aria-label="Foto de perfil" @change="onPhoto" />
              <small class="item-meta">Se recorta en cuadrado. Se guarda al pulsar «Guardar cambios».</small>
            </div>
          </div>

          <dl class="facts">
            <div><dt>Usuario</dt><dd>{{ profile.username }}</dd></div>
            <div><dt>Rol</dt><dd>{{ ROLE_NAMES[profile.role] || profile.role }}</dd></div>
            <div><dt>Alta</dt><dd>{{ formatEpochDate(profile.createdAt) }}</dd></div>
          </dl>
          <p class="item-meta">El usuario y el rol los cambia el administrador.</p>

          <div class="form-group">
            <label for="profile-name">Nombre y apellidos:</label>
            <input id="profile-name" v-model="name" type="text" required maxlength="100" autocomplete="name" />
          </div>

          <button type="submit" :disabled="savingData">Guardar cambios</button>
          <p v-if="dataSuccess" class="msg-success" role="status">{{ dataSuccess }}</p>
        </form>
        <p v-if="dataError" class="msg-error" role="alert">{{ dataError }}</p>
      </section>

      <section v-if="activeTab === 'clave'" id="panel-clave" role="tabpanel" aria-labelledby="tab-clave" class="panel form-tab">
        <form @submit.prevent="changePassword" class="profile-form">
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
          <button type="submit" :disabled="savingPassword">Cambiar contraseña</button>
          <p v-if="passwordSuccess" class="msg-success" role="status">{{ passwordSuccess }}</p>
          <p v-if="passwordError" class="msg-error" role="alert">{{ passwordError }}</p>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.form-tab {
  max-width: 560px;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.photo-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
}

.photo-actions {
  display: flex;
  flex: 1 1 200px;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.photo-actions small {
  flex-basis: 100%;
}

.photo-input {
  display: none;
}

.facts {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin: 0;
}

.facts dt {
  color: var(--gargantua-gray);
  font-size: 0.85rem;
}

.facts dd {
  margin: 2px 0 0;
  font-weight: 600;
  overflow-wrap: anywhere;
}
</style>
