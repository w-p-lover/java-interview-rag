<script setup lang="ts">
import {
  AlertTriangle,
  CalendarDays,
  ChevronRight,
  ListChecks,
  RefreshCw,
  Route,
} from "@lucide/vue";
import { computed } from "vue";

import { useInterviewReviewPath } from "@/composables/useInterviewReviewPath";

const props = defineProps<{
  sessionIds: readonly string[];
}>();

const {
  result,
  loading,
  error,
  canGenerate,
  generate,
} = useInterviewReviewPath(() => props.sessionIds);

const recommendations = computed(() => result.value?.recommendations ?? []);
const actionLabel = computed(() => loading.value
  ? "正在整理…"
  : result.value ? "重新生成路径" : "生成学习路径");

function formatScore(score: number | null): string {
  return score == null ? "未练习" : `${score} 分`;
}

function formatDate(date: string): string {
  return date.replaceAll("-", ".");
}

function emitGenerate(): void {
  void generate();
}
</script>

<template>
  <section class="review-path" aria-labelledby="review-path-title">
    <header class="review-path__heading">
      <div class="review-path__title">
        <span class="review-path__mark" aria-hidden="true"><Route :size="18" /></span>
        <div>
          <h2 id="review-path-title">跨会话学习路径</h2>
          <p>按已有复习记录确定性安排下一次练习，不调用模型。</p>
        </div>
      </div>
      <button
        type="button"
        class="review-path__action"
        :disabled="!canGenerate || loading"
        @click="emitGenerate"
      >
        <RefreshCw v-if="result" :size="16" aria-hidden="true" />
        <Route v-else :size="16" aria-hidden="true" />
        <span>{{ actionLabel }}</span>
      </button>
    </header>

    <p class="review-path__scope">
      当前使用最近 {{ props.sessionIds.length }} 场面试中的复习任务和回答次数，最多展示 20 条待复习主题。
    </p>

    <p v-if="!props.sessionIds.length" class="review-path__empty">
      <ListChecks :size="17" aria-hidden="true" />
      <span>完成一场面试并生成复习任务后，这里会出现跨会话推荐。</span>
    </p>

    <div v-if="loading" class="review-path__loading" aria-live="polite" aria-busy="true">
      正在合并最近会话的复习记录…
    </div>

    <p v-if="error" class="review-path__error" role="alert">
      <AlertTriangle :size="16" aria-hidden="true" />
      <span>{{ error }}</span>
    </p>

    <template v-if="result && !loading">
      <p v-if="!recommendations.length" class="review-path__empty">
        <ListChecks :size="17" aria-hidden="true" />
        <span>最近会话没有待复习任务。</span>
      </p>
      <ol v-else class="review-path__list">
        <li v-for="(item, index) in recommendations" :key="`${item.sessionId}-${item.taskId}`">
          <div class="review-path__rank" aria-hidden="true">{{ index + 1 }}</div>
          <div class="review-path__copy">
            <div class="review-path__item-heading">
              <strong>{{ item.objective }}</strong>
              <span>{{ item.attemptCount ? `最近 ${formatScore(item.latestOverallScore)}` : "尚未练习" }}</span>
            </div>
            <p>{{ item.question }}</p>
            <small>{{ item.reason }}</small>
            <div class="review-path__meta">
              <span><CalendarDays :size="14" aria-hidden="true" /> {{ formatDate(item.recommendedReviewDate) }}</span>
              <span><ChevronRight :size="14" aria-hidden="true" /> 间隔 {{ item.intervalDays }} 天</span>
            </div>
          </div>
        </li>
      </ol>
    </template>
  </section>
</template>

<style scoped lang="scss">
.review-path {
  display: grid;
  gap: var(--space-md);
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.review-path__heading,
.review-path__title,
.review-path__action,
.review-path__error,
.review-path__empty,
.review-path__meta,
.review-path__item-heading {
  display: flex;
  align-items: center;
}

.review-path__heading {
  justify-content: space-between;
  gap: var(--space-md);
}

.review-path__title {
  align-items: flex-start;
  gap: var(--space-sm);
}

.review-path__mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.review-path__title div {
  display: grid;
  gap: var(--space-2xs);
}

.review-path__title h2,
.review-path__title p,
.review-path__scope,
.review-path__empty,
.review-path__loading,
.review-path__error,
.review-path__item-heading,
.review-path__copy p,
.review-path__copy small {
  margin: 0;
}

.review-path__title h2 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.review-path__title p,
.review-path__scope,
.review-path__empty,
.review-path__loading {
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.55;
}

.review-path__action {
  flex: 0 0 auto;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: transparent;
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 700;
}

.review-path__action:disabled {
  border-color: var(--color-rule-2);
  color: var(--color-faint);
  cursor: not-allowed;
}

.review-path__action:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.review-path__empty,
.review-path__loading,
.review-path__error {
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
}

.review-path__empty,
.review-path__loading {
  background: var(--color-paper-2);
}

.review-path__error {
  color: var(--color-error);
  background: var(--color-error-soft);
}

.review-path__list {
  display: grid;
  gap: var(--space-sm);
  margin: 0;
  padding: 0;
  list-style: none;
  counter-reset: review-path;
}

.review-path__list li {
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr);
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.review-path__rank {
  display: grid;
  place-items: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
  font-size: var(--text-sm);
  font-weight: 800;
}

.review-path__copy {
  display: grid;
  gap: var(--space-2xs);
  min-width: 0;
}

.review-path__item-heading {
  justify-content: space-between;
  gap: var(--space-sm);
}

.review-path__item-heading strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.review-path__item-heading span,
.review-path__copy small {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.review-path__copy p {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.review-path__meta {
  flex-wrap: wrap;
  gap: var(--space-sm);
  color: var(--color-accent-strong);
  font-size: var(--text-xs);
  font-weight: 700;
}

.review-path__meta span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2xs);
}

@media (max-width: 48rem) {
  .review-path__heading {
    align-items: stretch;
    flex-direction: column;
  }

  .review-path__action {
    width: 100%;
  }

  .review-path__item-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
