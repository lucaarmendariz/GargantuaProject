<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import TabMenu from '../components/TabMenu.vue';
import { useAction } from '../composables/useAction';
import { useTabRoute } from '../composables/useTabRoute';
import { formatEpochDate } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { getUser } from '../utils/session';

// Panel del cliente (ADMIN): qué ve cada rol y quién usa la app.
const router = useRouter();
const tabs = [
  { id: 'permisos', label: 'Permisos por rol' },
  { id: 'usuarios', label: 'Usuarios' },
  { id: 'nuevo', label: 'Nuevo usuario' },
];
const activeTab = useTabRoute(tabs);

const ROLES = ['ENCARGADO', 'OPERARIO'];
const USER_ROLES = ['ADMIN', 'ENCARGADO', 'OPERARIO'];
const ACCESS = [
  { value: 'NINGUNO', label: 'Sin acceso' },
  { value: 'VER', label: 'Ver (consultar y operar)' },
  { value: 'GESTIONAR', label: 'Gestionar (crear, editar, borrar)' },
];
const me = getUser()?.username;

const modules = ref([]);
const permissions = ref([]);
const users = ref([]);
const loadError = ref('');

// El administrador gestiona todos los módulos contratados: los demás le salen como NINGUNO
const contracted = computed(() => modules.value.filter((module) => module.access !== 'NINGUNO'));
const notContracted = computed(() => modules.value.filter((module) => module.access === 'NINGUNO'));
const accessOf = (role, moduleCode) =>
  permissions.value.find((p) => p.role === role && p.moduleCode === moduleCode)?.access || 'NINGUNO';

const load = async () => {
  try {
    const [myAccess, perms, list] = await Promise.all([
      apiClient.get('/me/permisos'),
      apiClient.get('/admin/permisos'),
      apiClient.get('/admin/usuarios'),
    ]);
    modules.value = myAccess.modules;
    permissions.value = perms;
    users.value = list;
    loadError.value = '';
  } catch (error) {
    loadError.value = await errorMessage(error, 'No se pudo cargar el panel.');
  }
};

const { busy, actionError, actionSuccess, run } = useAction(load);

const setPermission = (role, module, access) => run(
  () => apiClient.put(`/admin/permisos/${role}/${module.moduleCode}`, { access }),
  `${role}: ${module.name} → ${ACCESS.find((a) => a.value === access).label}.`,
  'No se pudo guardar el permiso.'
);

const updateUser = (user, changes, message) => run(
  () => apiClient.put(`/admin/usuarios/${user.userId}`, changes),
  message,
  'No se pudo actualizar el usuario.'
);

// --- Alta de usuario ---
const emptyForm = () => ({ username: '', name: '', role: 'OPERARIO', password: '' });
const form = ref(emptyForm());

const createUser = () => run(async () => {
  await apiClient.post('/admin/usuarios', form.value);
  form.value = emptyForm();
  activeTab.value = 'usuarios';
}, `Usuario ${form.value.username} creado. Ya puede entrar con la contraseña indicada.`, 'No se pudo crear el usuario.');

onMounted(load);
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Administración</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <TabMenu v-model="activeTab" :tabs="tabs" label="Acciones de administración" />

      <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
      <p v-if="actionError || loadError" class="msg-error" role="alert">{{ actionError || loadError }}</p>

      <!-- Permisos por rol -->
      <section v-if="activeTab === 'permisos'" id="panel-permisos" role="tabpanel" aria-labelledby="tab-permisos" class="panel">
        <h3>Qué puede hacer cada rol</h3>
        <p class="item-meta section-help">
          Los administradores gestionan todos los módulos. Los cambios se aplican al momento, también a quien ya esté dentro.
        </p>
        <div class="table-responsive">
          <table class="perm-table">
            <thead>
              <tr>
                <th scope="col">Módulo</th>
                <th v-for="role in ROLES" :key="role" scope="col">{{ role }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="module in contracted" :key="module.moduleCode">
                <th scope="row">{{ module.name }}</th>
                <td v-for="role in ROLES" :key="role" :data-label="role">
                  <select :value="accessOf(role, module.moduleCode)" :disabled="busy"
                    :aria-label="`${module.name}, ${role}`"
                    @change="setPermission(role, module, $event.target.value)">
                    <option v-for="option in ACCESS" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="notContracted.length" class="item-meta section-help">
          No contratados (no los ve nadie): {{ notContracted.map((m) => m.name).join(', ') }}.
        </p>
      </section>

      <!-- Usuarios -->
      <section v-if="activeTab === 'usuarios'" id="panel-usuarios" role="tabpanel" aria-labelledby="tab-usuarios" class="panel">
        <h3>Usuarios de la planta</h3>
        <p v-if="users.length === 0" class="empty-state">Todavía no hay usuarios.</p>
        <ul v-else class="user-list">
          <li v-for="user in users" :key="user.userId" class="item-card user-row" :class="{ inactive: !user.active }">
            <div class="user-info">
              <span class="item-title">{{ user.name }} <span v-if="user.username === me" class="item-meta">(tú)</span></span>
              <span class="item-meta">{{ user.username }} · alta {{ formatEpochDate(user.createdAt) }}</span>
            </div>
            <select :value="user.role" :disabled="busy || user.username === me" :aria-label="`Rol de ${user.name}`"
              @change="updateUser(user, { role: $event.target.value }, `${user.name} ahora es ${$event.target.value}.`)">
              <option v-for="role in USER_ROLES" :key="role" :value="role">{{ role }}</option>
            </select>
            <span class="status" :class="user.active ? 'status-ENTREGADO' : 'status-CANCELADO'">{{ user.active ? 'ACTIVO' : 'DESACTIVADO' }}</span>
            <button v-if="user.username !== me" :disabled="busy" :class="user.active ? 'btn-danger' : 'btn-secondary'"
              @click="updateUser(user, { active: !user.active }, `${user.name} ${user.active ? 'desactivado' : 'activado'}.`)">
              {{ user.active ? 'Desactivar' : 'Activar' }}
            </button>
          </li>
        </ul>
      </section>

      <!-- Nuevo usuario -->
      <section v-if="activeTab === 'nuevo'" id="panel-nuevo" role="tabpanel" aria-labelledby="tab-nuevo" class="panel form-tab">
        <h3>Nuevo usuario</h3>
        <form @submit.prevent="createUser" class="user-form">
          <div class="form-group">
            <label for="new-name">Nombre y apellidos (*):</label>
            <input id="new-name" v-model="form.name" type="text" required autocomplete="off" />
          </div>
          <div class="form-group">
            <label for="new-username">Usuario para entrar (*):</label>
            <input id="new-username" v-model="form.username" type="text" required autocomplete="off" />
          </div>
          <div class="form-group">
            <label for="new-role">Rol (*):</label>
            <select id="new-role" v-model="form.role">
              <option v-for="role in USER_ROLES" :key="role" :value="role">{{ role }}</option>
            </select>
          </div>
          <div class="form-group">
            <label for="new-password">Contraseña inicial (*):</label>
            <input id="new-password" v-model="form.password" type="password" required minlength="8" autocomplete="new-password" />
            <small class="item-meta">Mínimo 8 caracteres. Dásela a la persona en mano.</small>
          </div>
          <button type="submit" :disabled="busy">Crear usuario</button>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 8px;
}

.section-help {
  margin-bottom: 14px;
}

.table-responsive {
  overflow-x: auto;
}

.perm-table {
  width: 100%;
  border-collapse: collapse;
}

.perm-table th,
.perm-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--gargantua-border);
  text-align: left;
}

.perm-table thead th {
  background: var(--gargantua-cream);
}

.user-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.user-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  text-align: left;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  flex: 1 1 220px;
  flex-direction: column;
  gap: 2px;
}

.user-row select {
  flex: 0 1 180px;
}

.user-row.inactive {
  opacity: 0.7;
}

.form-tab {
  max-width: 760px;
}

.user-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* En el móvil cada módulo de la tabla de permisos es una ficha */
@media (max-width: 600px) {
  .perm-table thead {
    display: none;
  }

  .perm-table tr,
  .perm-table th,
  .perm-table td {
    display: block;
    width: 100%;
  }

  .perm-table tr {
    padding: 8px 0;
    border-bottom: 1px solid var(--gargantua-border);
  }

  .perm-table th,
  .perm-table td {
    border: 0;
    padding: 4px 0;
  }

  .perm-table td::before {
    content: attr(data-label);
    display: block;
    color: var(--gargantua-gray);
    font-size: 0.85rem;
    font-weight: 600;
  }

  .user-row select,
  .user-row button {
    flex: 1 1 100%;
  }
}
</style>
