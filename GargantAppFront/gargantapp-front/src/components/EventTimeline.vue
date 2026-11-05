<script setup>
defineProps({
  items: { type: Array, required: true }
});
</script>

<template>
  <div class="timeline">
    <div v-for="item in items" :key="item.key" class="timeline-item">
      <div class="timeline-badge"></div>
      <div class="timeline-panel">
        <div class="timeline-heading">
          <span class="action-title">{{ item.title }}</span>
          <span class="action-time">{{ item.time }}</span>
        </div>
        <div class="timeline-body">
          <p v-for="row in item.rows" :key="row.label"><strong>{{ row.label }}:</strong> {{ row.value }}</p>
          <div v-if="item.code" class="ocr-dump">
            <code>{{ item.code }}</code>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline {
  position: relative;
}

.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 6px;
  width: 2px;
  background: var(--gargantua-border);
}

.timeline-item {
  position: relative;
  display: flex;
  gap: clamp(12px, 3vw, 20px);
  margin-bottom: 16px;
}

.timeline-badge {
  z-index: 1;
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  margin-top: 18px;
  background: var(--gargantua-gold);
  border: 2px solid var(--gargantua-dark);
  border-radius: 50%;
}

.timeline-panel {
  flex: 1;
  min-width: 0;
  padding: 16px;
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.timeline-heading {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 2px 12px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--gargantua-border);
}

.action-title {
  color: var(--gargantua-dark);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.action-time {
  color: var(--gargantua-gray);
  font-size: 0.85rem;
}

.timeline-body p {
  margin: 4px 0;
  font-size: 0.925rem;
  overflow-wrap: anywhere;
}

.timeline-body strong {
  font-weight: 600;
}

.ocr-dump {
  margin-top: 8px;
  padding: 6px 10px;
  background: var(--gargantua-cream);
  border-radius: var(--radius-sm);
}
</style>
