<script setup>
import { computed } from 'vue';

// Foto de perfil, o las iniciales del nombre si no hay foto
const props = defineProps({
  src: { type: String, default: null },
  name: { type: String, default: '' },
  size: { type: Number, default: 36 },
});

const initials = computed(() => props.name.trim().split(/\s+/).slice(0, 2).map((word) => word[0] || '').join('').toUpperCase());
</script>

<template>
  <img v-if="src" :src="src" alt="" class="avatar" :style="{ width: `${size}px`, height: `${size}px` }" />
  <span v-else class="avatar avatar-initials" aria-hidden="true"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${Math.round(size * 0.4)}px` }">{{ initials }}</span>
</template>

<style scoped>
.avatar {
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-initials {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--gargantua-gold);
  color: var(--gargantua-dark);
  font-weight: 700;
}
</style>
