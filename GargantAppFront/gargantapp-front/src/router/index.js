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
import AdministracionView from '../views/AdministracionView.vue'
import PlataformaView from '../views/PlataformaView.vue'
import CuentaView from '../views/CuentaView.vue'
import { can, getToken, getUser, hasRole } from '../utils/session';
import { refreshAccess } from '../utils/access';

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
      meta: { requiresAuth: true, module: 'INVENTARIO' }
    },
    { 
      path: '/historial-global', 
      name: 'historial', 
      component: HistorialGlobalView,
      meta: { requiresAuth: true, module: 'HISTORIAL' }
    },
    { 
      path: '/produccion', 
      name: 'produccion', 
      component: ProduccionView,
      meta: { requiresAuth: true, module: 'PRODUCCION' }
    },
    {
      path: '/pedidos',
      name: 'pedidos',
      component: PedidosView,
      meta: { requiresAuth: true, module: 'PEDIDOS' }
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
      meta: { requiresAuth: true, module: 'PEDIDOS' }
    },
    {
      path: '/lotes',
      name: 'lotes',
      component: LotesView,
      meta: { requiresAuth: true, module: 'LOTES' }
    },
    {
      path: '/lotes/:id/trazabilidad',
      name: 'lote-trazabilidad',
      component: TrazabilidadLoteView,
      props: true,
      meta: { requiresAuth: true }
    },
    {
      // Panel del cliente: permisos por rol y usuarios
      path: '/administracion',
      name: 'administracion',
      component: AdministracionView,
      meta: { requiresAuth: true, roles: ['ADMIN', 'SUPERADMIN'] }
    },
    {
      // Panel del proveedor: módulos contratados
      path: '/plataforma',
      name: 'plataforma',
      component: PlataformaView,
      meta: { requiresAuth: true, roles: ['SUPERADMIN'] }
    },
    {
      // Cualquier usuario cambia su propia contraseña
      path: '/cuenta',
      name: 'cuenta',
      component: CuentaView,
      meta: { requiresAuth: true }
    }
  ]
})

// Guardián de Seguridad Global (Navigation Guard)
router.beforeEach(async (to) => {
  // 1. Recuperamos los datos de sesión almacenados al loguearse
  const user = getUser();

  // 2. Comprobar si la pestaña exige estar logueado
  if (to.meta.requiresAuth && (!user || !getToken())) {
    // Si no hay sesión, patada hacia el login
    return { name: 'login' };
  }

  // 3. Permisos al día (una vez por carga de la página) y restricciones por rol o por módulo
  if (to.meta.requiresAuth) await refreshAccess();
  if ((to.meta.roles && !hasRole(...to.meta.roles)) || (to.meta.module && !can(to.meta.module))) {
    // Si está logueado pero no tiene permiso, le mandamos al Panel Central
    alert('Acceso denegado: tu perfil no tiene permiso para esta sección.');
    return { name: 'dashboard' };
  }

  // Si pasa todos los controles, se le permite continuar el viaje
})

export default router