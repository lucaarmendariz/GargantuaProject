<script setup>
// Iconos de línea de los módulos del Panel Central. Toman el color del texto (currentColor).
defineProps({
  name: { type: String, required: true },
  size: { type: Number, default: 28 },
});
</script>

<template>
  <svg class="module-icon" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    <!-- Pedidos: portapapeles con lista -->
    <template v-if="name === 'pedidos'">
      <rect x="8" y="2" width="8" height="4" rx="1" />
      <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
      <path d="M12 11h4M12 16h4M8 11h.01M8 16h.01" />
    </template>
    <!-- Inventario: paquete -->
    <template v-else-if="name === 'inventario'">
      <path d="M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z" />
      <path d="M12 22V12M3.3 7 12 12l8.7-5M7.5 4.27l9 5.15" />
    </template>
    <!-- Lotes: etiqueta -->
    <template v-else-if="name === 'lotes'">
      <path d="M12.59 2.59A2 2 0 0 0 11.17 2H4a2 2 0 0 0-2 2v7.17a2 2 0 0 0 .59 1.42l8.7 8.7a2.43 2.43 0 0 0 3.42 0l6.58-6.58a2.43 2.43 0 0 0 0-3.42z" />
      <circle cx="7.5" cy="7.5" r="1.5" />
    </template>
    <!-- Historial: reloj con flecha hacia atrás -->
    <template v-else-if="name === 'historial'">
      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
      <path d="M3 3v5h5M12 7v5l4 2" />
    </template>
    <!-- Producción: fábrica -->
    <template v-else-if="name === 'produccion'">
      <path d="M2 20a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8l-7 5V8l-7 5V4a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z" />
      <path d="M7 18h1M12 18h1M17 18h1" />
    </template>
    <!-- Administración: usuarios -->
    <template v-else-if="name === 'administracion'">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
    </template>
    <!-- Plataforma: interruptores -->
    <template v-else-if="name === 'plataforma'">
      <rect x="2" y="5" width="20" height="6" rx="3" />
      <circle cx="17" cy="8" r="1.5" />
      <rect x="2" y="13" width="20" height="6" rx="3" />
      <circle cx="7" cy="16" r="1.5" />
    </template>
    <!-- Calendario -->
    <template v-else-if="name === 'calendario'">
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </template>
  </svg>
</template>

<style scoped>
.module-icon {
  flex-shrink: 0;
}
</style>
