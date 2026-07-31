<script setup lang="ts">
import { computed, shallowRef } from "vue";

import type { InterviewReviewAttempt } from "@/api/interview";

const props = defineProps<{
  taskId: string;
  attempts: InterviewReviewAttempt[];
  loading: boolean;
  submitting: boolean;
  error: string;
}>();

const emit = defineEmits<{
  submit: [answer: string];
}>();

const answer = shallowRef("");
const canSubmit = computed(() => answer.value.trim().length > 0 && !props.submitting);
const answerFieldId = computed(() => `review-attempt-answer-${props.taskId}`);

function submit(): void {
  const normalized = answer.value.trim();
  if (!normalized || props.submitting) {
    return;
  }
  emit("submit", normalized);
  answer.value = "";
}

function formatDate(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", {
      dateStyle: "medium",
      timeStyle: "short",
      hour12: false,
    }).format(parsed);
}

function formatDelta(value: number | null): string {
  if (value === null) {
    return "—";
  }
  return value > 0 ? `+${value}` : String(value);
}
</script>

<template>
  <section class="attempt-panel" aria-label="复习二次作答">
    <div class="attempt-heading">
      <div>
        <strong>再次作答</strong>
        <p>结合遗漏项，重新组织一次回答。</p>
      </div>
      <span>{{ attempts.length ? `已回答 ${attempts.length} 次` : "尚未作答" }}</span>
    </div>

    <form class="attempt-form" @submit.prevent="submit">
      <label :for="answerFieldId">你的复习回答</label>
      <textarea
        :id="answerFieldId"
        v-model="answer"
        rows="4"
        maxlength="10000"
        placeholder="先说结论，再补充之前遗漏的要点。"
      ></textarea>
      <div class="attempt-actions">
        <small>提交后可查看与之前回答的分数变化。</small>
        <button type="submit" :disabled="!canSubmit">
          {{ submitting ? "正在提交…" : "提交回答" }}
        </button>
      </div>
    </form>

    <p v-if="error" class="attempt-error" role="alert">{{ error }}</p>
    <p v-if="loading" class="attempt-loading" aria-live="polite">正在加载历史回答…</p>

    <ol v-if="attempts.length" class="attempt-list">
      <li v-for="attempt in attempts" :key="attempt.id" class="attempt-item">
        <div class="attempt-meta">
          <strong>第 {{ attempt.attemptNumber }} 次复习回答</strong>
          <span>{{ formatDate(attempt.createdAt) }}</span>
        </div>
        <p class="attempt-answer">{{ attempt.answer }}</p>
        <div class="attempt-score-row">
          <span>本次 {{ attempt.evaluation.overallScore }} 分</span>
          <span>较原回答 {{ formatDelta(attempt.scoreDeltaFromOriginal) }}</span>
          <span v-if="attempt.previousOverallScore !== null">
            较上次 {{ formatDelta(attempt.scoreDeltaFromPrevious) }}
          </span>
        </div>
        <p class="attempt-feedback">{{ attempt.evaluation.improvementSuggestion }}</p>
        <details class="attempt-version">
          <summary>评价信息</summary>
          <small>{{ attempt.promptVersion }} · {{ attempt.rubricVersion }} · {{ attempt.modelName }}</small>
        </details>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.attempt-panel,
.attempt-heading,
.attempt-form,
.attempt-item {
  display: grid;
  gap: var(--space-sm);
}

.attempt-panel {
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.attempt-heading {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.attempt-heading p,
.attempt-heading span,
.attempt-actions small,
.attempt-meta span {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.attempt-heading strong,
.attempt-form label {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.attempt-heading p,
.attempt-answer,
.attempt-feedback {
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.6;
}

textarea {
  width: 100%;
  resize: vertical;
  padding: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-surface);
  font: inherit;
}

.attempt-actions,
.attempt-meta,
.attempt-score-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}

button {
  min-height: 2.5rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-ink);
  background: var(--color-accent);
  font-weight: 700;
  white-space: nowrap;
}

.attempt-error {
  margin: 0;
  padding: var(--space-sm);
  border: var(--rule-thin) solid var(--color-error-rule);
  border-radius: var(--radius-control);
  color: var(--color-error);
  background: var(--color-error-soft);
  font-size: var(--text-sm);
}

.attempt-loading {
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.attempt-list {
  display: grid;
  gap: var(--space-sm);
  margin: 0;
  padding: 0;
  list-style: none;
}

.attempt-item {
  gap: var(--space-md);
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.attempt-meta strong,
.attempt-score-row {
  color: var(--color-ink-2);
  font-size: var(--text-xs);
}

.attempt-version summary {
  width: fit-content;
  color: var(--color-muted);
  cursor: pointer;
  font-size: var(--text-xs);
}

.attempt-version small {
  display: block;
  margin-top: var(--space-xs);
  overflow-wrap: anywhere;
  color: var(--color-faint);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.attempt-score-row {
  justify-content: flex-start;
  flex-wrap: wrap;
  color: var(--color-success);
  font-family: var(--font-mono);
}

@media (max-width: 40rem) {
  .attempt-actions,
  .attempt-meta {
    align-items: stretch;
    flex-direction: column;
  }

  button {
    width: 100%;
  }
}
</style>
