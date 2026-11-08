<script setup>
import { nextTick, ref, watch } from 'vue';
import { answerConfirm, confirmState } from '../utils/confirm';

// <dialog> nativo con showModal(): bloquea el fondo, lleva el foco dentro y se cierra con Esc
const dialog = ref(null);
const cancelButton = ref(null);

watch(() => confirmState.open, async (open) => {
  if (open) {
    dialog.value?.showModal();
    // Lo seguro por defecto: el foco en Cancelar, para que un Enter despistado no borre nada
    await nextTick();
    cancelButton.value?.focus();
  } else if (dialog.value?.open) {
    dialog.value.close();
  }
});
</script>

<template>
  <dialog ref="dialog" class="confirm-dialog" aria-labelledby="confirm-title" aria-describedby="confirm-message"
    @cancel.prevent="answerConfirm(false)" @click.self="answerConfirm(false)">
    <div class="confirm-body">
      <h3 id="confirm-title">{{ confirmState.title }}</h3>
      <p v-if="confirmState.message" id="confirm-message">{{ confirmState.message }}</p>
      <div class="confirm-actions">
        <button ref="cancelButton" type="button" class="btn-secondary" @click="answerConfirm(false)">No, volver</button>
        <button type="button" :class="confirmState.danger ? 'btn-danger' : ''" @click="answerConfirm(true)">
          {{ confirmState.confirmText }}
        </button>
      </div>
    </div>
  </dialog>
</template>

<style scoped>
.confirm-dialog {
  width: min(440px, calc(100vw - 32px));
  padding: 0;
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  box-shadow: 0 12px 40px rgba(51, 33, 15, 0.25);
  color: var(--gargantua-text);
}

.confirm-dialog::backdrop {
  background: rgba(51, 33, 15, 0.45);
}

.confirm-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px;
}

.confirm-body p {
  margin: 0;
  color: var(--gargantua-gray);
  line-height: 1.5;
}

.confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

@media (max-width: 480px) {
  .confirm-actions button {
    flex: 1 1 100%;
  }
}
</style>
