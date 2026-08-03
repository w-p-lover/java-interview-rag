<script setup lang="ts">
import type { InterviewReviewSessionHistory } from "@/api/interview";

defineProps<{
  history: InterviewReviewSessionHistory;
}>();
</script>

<template>
  <section class="history-summary" aria-label="本场复习进度">
    <div>
      <strong>{{ history.practicedTaskCount }} / {{ history.totalTaskCount }}</strong>
      <span>已练题目</span>
    </div>
    <div>
      <strong>{{ history.totalAttemptCount }}</strong>
      <span>累计作答</span>
    </div>
    <div>
      <strong>{{ history.improvedTaskCount }}</strong>
      <span>较正式回答提升</span>
    </div>
  </section>
</template>

<style scoped lang="scss">
.history-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-block: var(--rule-thin) solid var(--color-rule);
}

.history-summary div {
  display: grid;
  gap: var(--space-xs);
  min-width: 0;
  padding: var(--space-md);
}

.history-summary div + div {
  border-left: var(--rule-thin) solid var(--color-rule);
}

.history-summary strong {
  color: var(--color-ink);
  font-family: var(--font-mono);
  font-size: var(--text-lg);
}

.history-summary span {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

@media (max-width: 30rem) {
  .history-summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .history-summary div + div {
    border-top: var(--rule-thin) solid var(--color-rule);
    border-left: 0;
  }
}
</style>
