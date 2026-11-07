import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import InventarioView from '../views/InventarioView.vue'
import HistorialGlobalView from '../views/HistorialGlobalView.vue'
import ProduccionView from '../views/ProduccionView.vue'
import PedidosView from '../views/PedidosView.vue'
import PedidoDetalleView from '../views/PedidoDetalleView.vue'
import TrazabilidadLoteView from '../views/TrazabilidadLoteView.vue'
import LotesView from '../views/LotesView.vue'
import { getToken, getUser, hasRole } from '../utils/session';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { 
      path: '/', 
      name: 'login', 
      component: LoginView 
    },
    { 
      path: '/dashboard', 
      name: 'dashboard', 
      component: DashboardView,
      meta: { requiresAuth: true } // Cualquiera logueado puede entrar
    },
    { 
      path: '/inventario', 
      name: 'inventario', 
      component: InventarioView,
      meta: { requiresAuth: true, roles: ['ADMIN', 'ENCARGADO'] } // Restringido
    },
    { 
      path: '/historial-global', 
      name: 'historial', 
      component: HistorialGlobalView,
      meta: { requiresAuth: true, roles: ['ADMIN'] } // Solo el Administrador supremo
    },
    { 
      path: '/produccion', 
      name: 'produccion', 
      component: ProduccionView,
      meta: { requiresAuth: true, roles: ['ADMIN', 'OPERARIO'] } // Operarios y admin
    },
    {
      path: '/pedidos',
      name: 'pedidos',
      component: PedidosView,
      meta: { requiresAuth: true }
    },
    {
      // Los pedidos cerrados son ahora una pestaña de Pedidos; la ruta antigua sigue funcionando
      path: '/pedidos/cerrados',
      redirect: { name: 'pedidos', query: { tab: 'cerrados' } }
    },
    {
      path: '/pedidos/:id',
      name: 'pedido-detalle',
      component: PedidoDetalleView,
      props: true,
      meta: { requiresAuth: true }
    },
    {
      path: '/lotes',
      name: 'lotes',
      component: LotesView,
      meta: { requiresAuth: true, roles: ['ADMIN', 'ENCARGADO'] } // Editar y eliminar lotes
    },
    {
      path: '/lotes/:id/trazabilidad',
      name: 'lote-trazabilidad',
      component: TrazabilidadLoteView,
      props: true,
      meta: { requiresAuth: true }
    }
  ]
})

// Guardián de Seguridad Global (Navigation Guard)
router.beforeEach((to) => {
  // 1. Recuperamos los datos de sesión almacenados al loguearse
  const user = getUser();

  // 2. Comprobar si la pestaña exige estar logueado
  if (to.meta.requiresAuth && (!user || !getToken())) {
    // Si no hay sesión, patada hacia el login
    return { name: 'login' };
  }

  // 3. Comprobar restricciones de Rol (Si la ruta tiene limitaciones)
  if (to.meta.roles && !hasRole(...to.meta.roles)) {
    // Si está logueado pero no tiene el rol necesario, le mandamos al Panel Central
    alert('Acceso denegado: Tu perfil de operario no tiene permisos para esta sección.');
    return { name: 'dashboard' };
  }

  // Si pasa todos los controles, se le permite continuar el viaje
})

export default router