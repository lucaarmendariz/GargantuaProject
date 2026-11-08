<script setup>
// Escáner con la cámara del móvil o del PC.
//  - Lee en directo códigos de barras y QR (Code128, EAN, QR, DataMatrix, GS1...).
//  - "Leer texto (OCR)" reconoce el texto de la etiqueta cuando no hay código.
//  - "Hacer una foto" funciona aunque la cámara en directo no esté disponible.
// Emite 'result' con { text, source: 'barcode' | 'ocr' } y 'close'.
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { BrowserMultiFormatReader } from '@zxing/browser';
import { DecodeHintType } from '@zxing/library';

defineProps({
  title: { type: String, default: 'Escanear etiqueta' },
});
const emit = defineEmits(['result', 'close']);

const video = ref(null);
const photoInput = ref(null);
const status = ref('Abriendo la cámara...');
const error = ref('');
const busy = ref(false);
const liveAvailable = ref(false);

const hints = new Map([[DecodeHintType.TRY_HARDER, true]]);
const reader = new BrowserMultiFormatReader(hints);
let controls = null;
let finished = false;
// El escáner se cerró mientras la cámara aún arrancaba (p. ej. con el aviso de permiso abierto)
let closed = false;

const finish = (text, source) => {
  if (finished || !text?.trim()) return;
  finished = true;
  stopCamera();
  if (navigator.vibrate) navigator.vibrate(80);
  emit('result', { text: text.trim(), source });
};

const stopCamera = () => {
  controls?.stop();
  controls = null;
};

const startCamera = async () => {
  // Safari en iPhone solo da acceso a la cámara en páginas HTTPS
  if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
    status.value = '';
    error.value = 'La cámara en directo necesita abrir la app con https://. Puedes usar "Hacer una foto".';
    return;
  }
  try {
    controls = await reader.decodeFromConstraints(
      { video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false },
      video.value,
      (result) => {
        if (result) finish(result.getText(), 'barcode');
      },
    );
    if (closed) {
      stopCamera();
      return;
    }
    liveAvailable.value = true;
    status.value = 'Apunta al código de barras o QR de la etiqueta.';
  } catch (e) {
    status.value = '';
    error.value = e?.name === 'NotAllowedError'
      ? 'No hay permiso para usar la cámara. Actívalo en Ajustes > Safari > Cámara, o usa "Hacer una foto".'
      : 'No se pudo abrir la cámara. Puedes usar "Hacer una foto".';
  }
};

// OCR con tesseract.js. Se carga solo cuando hace falta porque pesa bastante
// y descarga el idioma la primera vez (requiere internet).
const recognizeText = async (image) => {
  const { createWorker } = await import('tesseract.js');
  const worker = await createWorker('spa');
  try {
    const { data } = await worker.recognize(image);
    return data.text;
  } finally {
    await worker.terminate();
  }
};

const captureFrame = () => {
  const canvas = document.createElement('canvas');
  canvas.width = video.value.videoWidth;
  canvas.height = video.value.videoHeight;
  canvas.getContext('2d').drawImage(video.value, 0, 0);
  return canvas;
};

const runOcr = async (image) => {
  busy.value = true;
  error.value = '';
  status.value = 'Leyendo el texto de la etiqueta... (la primera vez tarda un poco más)';
  try {
    const text = await recognizeText(image);
    if (text.trim()) {
      finish(text, 'ocr');
    } else {
      status.value = '';
      error.value = 'No se ha reconocido texto. Acerca la cámara y evita reflejos.';
    }
  } catch {
    status.value = '';
    error.value = 'No se pudo leer el texto. Comprueba la conexión a internet e inténtalo de nuevo.';
  } finally {
    busy.value = false;
  }
};

const readTextFromVideo = () => {
  if (!video.value?.videoWidth) return;
  runOcr(captureFrame());
};

// Foto: primero se busca un código; si no hay, se lee el texto
const onPhoto = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  const url = URL.createObjectURL(file);
  busy.value = true;
  error.value = '';
  status.value = 'Buscando un código en la foto...';
  try {
    const result = await reader.decodeFromImageUrl(url);
    finish(result.getText(), 'barcode');
  } catch {
    await runOcr(url);
  } finally {
    busy.value = false;
    URL.revokeObjectURL(url);
  }
};

const close = () => {
  stopCamera();
  emit('close');
};

const onKeydown = (event) => {
  if (event.key === 'Escape') close();
};

onMounted(async () => {
  window.addEventListener('keydown', onKeydown);
  await nextTick();
  startCamera();
});

onBeforeUnmount(() => {
  closed = true;
  window.removeEventListener('keydown', onKeydown);
  stopCamera();
});
</script>

<template>
  <div class="scanner-backdrop" @click.self="close">
    <section class="scanner panel" role="dialog" aria-modal="true" aria-labelledby="scanner-title">
      <header class="scanner-head">
        <h3 id="scanner-title">{{ title }}</h3>
        <button type="button" class="btn-secondary" @click="close">Cerrar</button>
      </header>

      <div class="viewfinder" :class="{ hidden: !liveAvailable }">
        <video ref="video" playsinline muted autoplay></video>
        <div class="guide" aria-hidden="true"></div>
      </div>

      <p v-if="status" class="item-meta scanner-status" role="status">{{ status }}</p>
      <p v-if="error" class="msg-error" role="alert">{{ error }}</p>

      <div class="scanner-actions">
        <button v-if="liveAvailable" type="button" @click="readTextFromVideo" :disabled="busy">Leer texto (OCR)</button>
        <button type="button" class="btn-secondary" @click="photoInput.click()" :disabled="busy">Hacer una foto</button>
        <input ref="photoInput" type="file" accept="image/*" capture="environment" class="photo-input" @change="onPhoto" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.scanner-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(51, 33, 15, 0.6);
}

.scanner {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: min(100%, 560px);
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
}

.scanner-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.viewfinder {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: #000;
  aspect-ratio: 4 / 3;
}

.viewfinder.hidden {
  display: none;
}

.viewfinder video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Recuadro donde colocar la etiqueta */
.guide {
  position: absolute;
  inset: 22% 10%;
  border: 3px solid var(--gargantua-gold);
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.25);
}

.scanner-status {
  margin: 0;
}

.scanner-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.scanner-actions button {
  flex: 1 1 160px;
}

.photo-input {
  display: none;
}
</style>
