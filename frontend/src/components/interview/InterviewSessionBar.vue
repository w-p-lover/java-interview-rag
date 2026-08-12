<script setup lang="ts">
import { CheckCircle2, RotateCcw } from "@lucide/vue";

defineProps<{
  targetRole: string;
  status: "IN_PROGRESS" | "COMPLETED";
  currentQuestionIndex: number;
  questionCount: number;
  progress: number;
  loading: boolean;
}>();

const emit = defineEmits<{
  reset: [];
}>();
</script>

<template>
  <section id="interview-progress" class="session-bar" aria-label="当前面试进度">
    <div class="session-bar__main">
      <span class="session-bar__role">{{ targetRole }}</span>
      <strong>
        <CheckCircle2 v-if="status === 'COMPLETED'" :size="16" aria-hidden="true" />
        {{ status === "COMPLETED" ? "已完成" : "第 " + (currentQuestionIndex + 1) + " / " + questionCount + " 题" }}
      </strong>
    </div>

    <div class="session-bar__track" aria-hidden="true">
      <span :style="{ transform: `scaleX(${progress / 100})` }"></span>
    </div>

    <button type="button" :disabled="loading" @click="emit('reset')">
      <RotateCcw :size="16" aria-hidden="true" />
      <span>{{ status === "COMPLETED" ? "新面试" : "重新开始" }}</span>
    </button>
  </section>
</template>

<style scoped lang="scss">
.session-bar {
  display: grid;
  grid-template-columns: minmax(12rem, auto) minmax(8rem, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.session-bar__main {
  display: grid;
  gap: var(--space-3xs);
  min-width: 0;
}

.session-bar__role,
.session-bar__main strong,
button {
  white-space: nowrap;
}

.session-bar__role {
  overflow: hidden;
  color: var(--color-muted);
  font-size: var(--text-xs);
  text-overflow: ellipsis;
}

.session-bar__main strong {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.session-bar__main svg {
  color: var(--color-success);
}

.session-bar__track {
  height: var(--space-xs);
  overflow: hidden;
  border-radius: 0;
  background: var(--color-paper-3);
}

.session-bar__track span {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 0;
  background: var(--color-accent);
  transform-origin: left center;
  transition: transform var(--dur-long) var(--ease-out);
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-sm);
  border: 0;
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: transparent;
  font-size: var(--text-sm);
  font-weight: 700;
}

@media (hover: hover) and (pointer: fine) {
  button:hover:not(:disabled) {
    color: var(--color-ink);
    background: var(--color-paper-2);
  }
}

@media (max-width: 44rem) {
  .session-bar {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .session-bar__track {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  button span {
    display: none;
  }
}
</style>
