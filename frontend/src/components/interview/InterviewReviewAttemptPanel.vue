<script setup lang="ts">
import { ArrowUp } from "@lucide/vue";
import { computed, shallowRef } from "vue";

import type { InterviewReviewAttempt, InterviewReviewTaskHistory } from "@/api/interview";
import InterviewEvaluationEvidence from "@/components/interview/InterviewEvaluationEvidence.vue";

const props = defineProps<{
  taskId: string;
  attempts: InterviewReviewAttempt[];
  loading: boolean;
  submitting: boolean;
  error: string;
  summary: InterviewReviewTaskHistory | null;
}>();

const emit = defineEmits<{
  submit: [answer: string];
}>();

const answer = shallowRef("");
const canSubmit = computed(() => answer.value.trim().length > 0 && !props.submitting);
const answerFieldId = computed(() => `review-attempt-answer-${props.taskId}`);
const attemptRows = computed(() => {
  const comparisons = new Map((props.summary?.comparisons ?? [])
    .map((comparison) => [comparison.attemptId, comparison]));
  return props.attempts.map((attempt) => ({
    attempt,
    comparison: comparisons.get(attempt.id) ?? null,
  }));
});

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

    <div v-if="summary && summary.attemptCount" class="attempt-summary">
      <dl>
        <div>
          <dt>正式回答</dt>
          <dd>{{ summary.originalOverallScore }} 分</dd>
        </div>
        <div>
          <dt>首次复习</dt>
          <dd>{{ summary.firstOverallScore }} 分</dd>
        </div>
        <div>
          <dt>最近复习</dt>
          <dd>{{ summary.latestOverallScore }} 分</dd>
        </div>
        <div>
          <dt>最好成绩</dt>
          <dd>{{ summary.bestOverallScore }} 分</dd>
        </div>
      </dl>
      <div class="attempt-gaps">
        <strong>最近仍需补充</strong>
        <ul v-if="summary.unresolvedGaps.length">
          <li v-for="gap in summary.unresolvedGaps" :key="gap">{{ gap }}</li>
        </ul>
        <p v-else>最近一次评价没有记录新的遗漏。</p>
      </div>
      <a class="attempt-review-link" href="#review-assistant-title">
        <ArrowUp :size="16" aria-hidden="true" />
        返回复盘助手
      </a>
    </div>

    <ol v-if="attempts.length" class="attempt-list">
      <li v-for="row in attemptRows" :key="row.attempt.id" class="attempt-item">
        <div class="attempt-meta">
          <strong>第 {{ row.attempt.attemptNumber }} 次复习回答</strong>
          <span>{{ formatDate(row.attempt.createdAt) }}</span>
        </div>
        <p class="attempt-answer">{{ row.attempt.answer }}</p>
        <InterviewEvaluationEvidence :evaluation="row.attempt.evaluation" />
        <div class="attempt-score-row">
          <span>本次 {{ row.attempt.evaluation.overallScore }} 分</span>
          <span>较原回答 {{ formatDelta(row.attempt.scoreDeltaFromOriginal) }}</span>
          <span v-if="row.attempt.previousOverallScore !== null">
            较上次 {{ formatDelta(row.attempt.scoreDeltaFromPrevious) }}
          </span>
        </div>
        <div v-if="row.comparison" class="attempt-comparison">
          <strong>评分维度变化</strong>
          <div class="comparison-table-wrap">
            <table class="comparison-table">
              <thead>
                <tr>
                  <th scope="col">维度</th>
                  <th scope="col">较正式回答</th>
                  <th scope="col">较上次</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">技术准确性</th>
                  <td>{{ formatDelta(row.comparison.technicalDeltaFromOriginal) }}</td>
                  <td>{{ formatDelta(row.comparison.technicalDeltaFromPrevious) }}</td>
                </tr>
                <tr>
                  <th scope="row">完整性</th>
                  <td>{{ formatDelta(row.comparison.completenessDeltaFromOriginal) }}</td>
                  <td>{{ formatDelta(row.comparison.completenessDeltaFromPrevious) }}</td>
                </tr>
                <tr>
                  <th scope="row">表达清晰度</th>
                  <td>{{ formatDelta(row.comparison.communicationDeltaFromOriginal) }}</td>
                  <td>{{ formatDelta(row.comparison.communicationDeltaFromPrevious) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="comparison-gaps">
            <section v-if="row.comparison.originalGapsStillPresent.length" class="gap-group gap-group--warning">
              <strong>原遗漏仍出现</strong>
              <ul><li v-for="gap in row.comparison.originalGapsStillPresent" :key="gap">{{ gap }}</li></ul>
            </section>
            <section v-if="row.comparison.originalGapsNotRepeated.length" class="gap-group gap-group--success">
              <strong>原遗漏本次未再出现</strong>
              <ul><li v-for="gap in row.comparison.originalGapsNotRepeated" :key="gap">{{ gap }}</li></ul>
            </section>
            <section v-if="row.attempt.attemptNumber > 1 && row.comparison.previousGapsNotRepeated.length" class="gap-group">
              <strong>较上次未再出现</strong>
              <ul><li v-for="gap in row.comparison.previousGapsNotRepeated" :key="gap">{{ gap }}</li></ul>
            </section>
            <section v-if="row.comparison.newGapsSincePrevious.length" class="gap-group gap-group--warning">
              <strong>本次新增评价项</strong>
              <ul><li v-for="gap in row.comparison.newGapsSincePrevious" :key="gap">{{ gap }}</li></ul>
            </section>
          </div>
          <small class="comparison-note">评价项按规范化文本精确比较；措辞变化不等同于能力已经掌握。</small>
        </div>
        <p class="attempt-feedback">{{ row.attempt.evaluation.improvementSuggestion }}</p>
        <details class="attempt-version">
          <summary>评价信息</summary>
          <small>{{ row.attempt.promptVersion }} · {{ row.attempt.rubricVersion }} · {{ row.attempt.modelName }}</small>
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

.attempt-comparison {
  display: grid;
  gap: var(--space-sm);
  padding-block: var(--space-sm);
  border-block: var(--rule-thin) solid var(--color-rule);
}

.attempt-comparison > strong,
.gap-group strong {
  color: var(--color-ink-2);
  font-size: var(--text-xs);
}

.comparison-table-wrap {
  overflow-x: auto;
}

.comparison-table {
  width: 100%;
  border-collapse: collapse;
  color: var(--color-ink-2);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  text-align: left;
}

.comparison-table th,
.comparison-table td {
  padding: var(--space-xs) var(--space-sm);
  border-bottom: var(--rule-thin) solid var(--color-rule);
  white-space: nowrap;
}

.comparison-table thead th {
  color: var(--color-muted);
}

.comparison-gaps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm) var(--space-lg);
}

.gap-group {
  display: grid;
  align-content: start;
  gap: var(--space-xs);
}

.gap-group ul {
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}

.gap-group--warning strong {
  color: var(--color-warning);
}

.gap-group--success strong {
  color: var(--color-success);
}

.comparison-note {
  color: var(--color-faint);
  font-size: var(--text-xs);
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

.attempt-summary {
  display: grid;
  gap: var(--space-md);
  padding-block: var(--space-sm);
}

.attempt-summary dl {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
}

.attempt-summary dl div {
  display: grid;
  gap: var(--space-xs);
  padding: var(--space-sm);
}

.attempt-summary dl div + div {
  border-left: var(--rule-thin) solid var(--color-rule);
}

.attempt-summary dt,
.attempt-gaps strong {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.attempt-summary dd {
  margin: 0;
  color: var(--color-ink-2);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: 700;
}

.attempt-gaps {
  display: grid;
  gap: var(--space-xs);
}

.attempt-gaps ul,
.attempt-gaps p {
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.attempt-gaps ul {
  padding-left: var(--space-lg);
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

.attempt-review-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: start;
  gap: var(--space-xs);
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-size: var(--text-sm);
  font-weight: 700;
  text-decoration: none;
}

.attempt-review-link:hover {
  background: var(--color-accent-soft);
}

.attempt-review-link:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: 2px;
}

.attempt-score-row {
  justify-content: flex-start;
  flex-wrap: wrap;
  color: var(--color-success);
  font-family: var(--font-mono);
}

@media (max-width: 40rem) {
  .attempt-summary dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .attempt-summary dl div:nth-child(3) {
    border-left: 0;
  }

  .attempt-summary dl div:nth-child(n + 3) {
    border-top: var(--rule-thin) solid var(--color-rule);
  }

  .attempt-actions,
  .attempt-meta {
    align-items: stretch;
    flex-direction: column;
  }

  .comparison-gaps {
    grid-template-columns: 1fr;
  }

  .attempt-review-link {
    justify-self: stretch;
  }

  button {
    width: 100%;
  }
}
</style>
