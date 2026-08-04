<script setup lang="ts">
import { shallowRef, watch } from "vue";
import { CalendarCheck, X } from "@lucide/vue";

const props = defineProps<{
  taskId: string;
  plannedReviewDate: string | null;
  busy: boolean;
}>();

const emit = defineEmits<{
  schedule: [taskId: string, plannedReviewDate: string | null];
}>();

const draftDate = shallowRef("");

watch(
  () => props.plannedReviewDate,
  (value) => {
    draftDate.value = value ?? "";
  },
  { immediate: true },
);

function save(): void {
  if (!draftDate.value) {
    return;
  }
  emit("schedule", props.taskId, draftDate.value);
}

function clear(): void {
  emit("schedule", props.taskId, null);
}
</script>

<template>
  <form class="schedule-control" @submit.prevent="save">
    <label :for="`review-date-${taskId}`">计划复习</label>
    <div class="schedule-fields">
      <input
        :id="`review-date-${taskId}`"
        v-model="draftDate"
        type="date"
        :disabled="busy"
      />
      <button type="submit" :disabled="busy || !draftDate">
        <CalendarCheck :size="16" aria-hidden="true" />
        {{ busy ? "保存中…" : "保存日期" }}
      </button>
      <button
        v-if="plannedReviewDate"
        class="clear-schedule"
        type="button"
        :disabled="busy"
        title="清除计划日期"
        @click="clear"
      >
        <X :size="16" aria-hidden="true" />
        <span class="sr-only">清除计划日期</span>
      </button>
    </div>
  </form>
</template>

<style scoped lang="scss">
.schedule-control {
  display: grid;
  gap: var(--space-xs);
}

.schedule-control > label {
  color: var(--color-muted);
  font-size: var(--text-xs);
  font-weight: 700;
}

.schedule-fields {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.schedule-fields input,
.schedule-fields button {
  min-height: 2.75rem;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-surface);
}

.schedule-fields input {
  min-width: 9rem;
  padding-inline: var(--space-sm);
}

.schedule-fields button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  padding-inline: var(--space-md);
  border-color: var(--color-accent);
  color: var(--color-accent-strong);
  font-weight: 700;
  white-space: nowrap;
}

.schedule-fields .clear-schedule {
  width: 2.75rem;
  padding: 0;
  border-color: var(--color-rule-2);
  color: var(--color-muted);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 40rem) {
  .schedule-fields {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .schedule-fields input {
    flex: 1 1 100%;
    width: 100%;
  }

  .schedule-fields button:not(.clear-schedule) {
    flex: 1;
  }
}
</style>
