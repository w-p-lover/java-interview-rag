<script setup lang="ts">
import { TrendingUp } from "@lucide/vue";
import { computed } from "vue";

import type { InterviewReviewSessionHistory } from "@/api/interview";

const props = defineProps<{
  history: InterviewReviewSessionHistory;
}>();

const progress = computed(() => props.history.totalTaskCount
  ? Math.round((props.history.practicedTaskCount / props.history.totalTaskCount) * 100)
  : 0);
</script>

<template>
  <section class="history-summary" aria-label="本场复习进度">
    <div class="history-copy">
      <span class="history-icon" aria-hidden="true"><TrendingUp :size="18" /></span>
      <div>
        <strong>本场已练 {{ history.practicedTaskCount }} / {{ history.totalTaskCount }} 道</strong>
        <p>
          累计作答 {{ history.totalAttemptCount }} 次，
          {{ history.improvedTaskCount }} 道最近得分高于正式回答。
        </p>
      </div>
    </div>
    <div
      class="history-track"
      role="progressbar"
      aria-label="本场复习题目完成进度"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="progress"
    >
      <span :style="{ width: `${progress}%` }"></span>
    </div>
  </section>
</template>

<style scoped lang="scss">
.history-summary {
  display: grid;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-radius: var(--radius-control);
  background: var(--color-accent-soft);
}

.history-copy {
  display: flex;
  align-items: start;
  gap: var(--space-md);
}

.history-icon {
  display: grid;
  flex: 0 0 auto;
  width: 2.25rem;
  height: 2.25rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-accent-ink);
  background: var(--color-accent);
}

.history-copy > div {
  display: grid;
  gap: var(--space-2xs);
}

.history-copy strong {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.history-copy p {
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.history-track {
  height: var(--space-xs);
  overflow: hidden;
  border-radius: var(--radius-round);
  background: var(--color-paper-3);
}

.history-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-accent);
  transition: width var(--dur-long) var(--ease-out);
}
</style>
