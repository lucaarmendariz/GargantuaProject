// Sesión del usuario guardada en el navegador tras el login.
const TOKEN_KEY = 'token';
const USER_KEY = 'user';
const ACCESS_KEY = 'access';
const LEVELS = ['NINGUNO', 'VER', 'GESTIONAR'];

export function saveSession({ token, ...user }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser() {
  const savedUser = localStorage.getItem(USER_KEY);
  return savedUser ? JSON.parse(savedUser) : null;
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ACCESS_KEY);
}

export function hasRole(...roles) {
  return roles.includes(getUser()?.role);
}

// Permisos por módulo que devuelve /api/me/permisos. Solo deciden qué se muestra:
// quien de verdad permite o rechaza cada acción es la API.

// El rol también puede haber cambiado desde el panel del administrador
export function saveAccess({ role, modules }) {
  localStorage.setItem(ACCESS_KEY, JSON.stringify(modules));
  const user = getUser();
  if (user) localStorage.setItem(USER_KEY, JSON.stringify({ ...user, role }));
}

// ¿Tiene al menos ese nivel en el módulo? can('PEDIDOS') = puede verlo; can('PEDIDOS', 'GESTIONAR') = puede cambiarlo
export function can(moduleCode, level = 'VER') {
  let modules = [];
  try {
    modules = JSON.parse(localStorage.getItem(ACCESS_KEY)) || [];
  } catch {
    // Permisos guardados ilegibles: sin permisos hasta la próxima carga
  }
  const access = modules.find((module) => module.moduleCode === moduleCode)?.access || 'NINGUNO';
  return LEVELS.indexOf(access) >= LEVELS.indexOf(level);
}
