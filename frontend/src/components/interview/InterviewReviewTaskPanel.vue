<script setup lang="ts">
import { computed } from "vue";

import type {
  InterviewReviewSessionHistory,
  InterviewReviewTask,
  InterviewReviewTaskQuery,
} from "@/api/interview";
import InterviewReviewAttemptPanel from "@/components/interview/InterviewReviewAttemptPanel.vue";
import InterviewReviewHistorySummary from "@/components/interview/InterviewReviewHistorySummary.vue";
import InterviewReviewScheduleControl from "@/components/interview/InterviewReviewScheduleControl.vue";
import InterviewReviewTaskFilters from "@/components/interview/InterviewReviewTaskFilters.vue";

const props = defineProps<{
  tasks: InterviewReviewTask[];
  loading: boolean;
  error: string;
  generated: boolean;
  completingTaskId: string;
  schedulingTaskId: string;
  actionError: string;
  query: InterviewReviewTaskQuery;
  attemptsByTaskId: Record<string, import("@/api/interview").InterviewReviewAttempt[]>;
  attemptsLoadingTaskId: string;
  submittingAttemptTaskId: string;
  attemptError: string;
  history: InterviewReviewSessionHistory | null;
}>();

const historiesByTaskId = computed(() => new Map(
  (props.history?.tasks ?? []).map((history) => [history.reviewTaskId, history]),
));

const emit = defineEmits<{
  retry: [];
  complete: [taskId: string];
  filter: [query: InterviewReviewTaskQuery];
  schedule: [taskId: string, plannedReviewDate: string | null];
  submitAttempt: [taskId: string, answer: string];
}>();

function formatCompletedAt(completedAt: string | null): string {
  if (!completedAt) {
    return "";
  }
  const parsed = new Date(completedAt);
  if (Number.isNaN(parsed.getTime())) {
    return completedAt;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
    hour12: false,
  }).format(parsed);
}

function formatPlannedDate(plannedReviewDate: string | null): string {
  if (!plannedReviewDate) {
    return "";
  }
  const parsed = new Date(`${plannedReviewDate}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return plannedReviewDate;
  }
  return new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium" }).format(parsed);
}

function handleSchedule(taskId: string, plannedReviewDate: string | null): void {
  emit("schedule", taskId, plannedReviewDate);
}
</script>

<template>
  <section class="review-card" aria-labelledby="review-task-title">
    <header class="review-header">
      <div>
        <h2 id="review-task-title">复习题目</h2>
        <p>根据本场回答中的遗漏整理。</p>
      </div>
    </header>

    <div v-if="loading" class="review-state" aria-live="polite">
      正在整理本场复习任务…
    </div>

    <div v-else-if="error && !tasks.length" class="review-state review-error" role="alert">
      <div>
        <strong>复习题目暂未就绪</strong>
        <p>{{ error }}</p>
      </div>
      <button type="button" @click="emit('retry')">重新整理</button>
    </div>

    <p v-if="actionError && tasks.length" class="review-action-error" role="alert">
      {{ actionError }}
    </p>

    <p v-if="error && tasks.length" class="review-action-error" role="alert">
      {{ error }}
    </p>

    <InterviewReviewHistorySummary
      v-if="history && history.totalTaskCount"
      :history="history"
    />

    <InterviewReviewTaskFilters
      v-if="history && history.totalTaskCount"
      :query="query"
      :busy="loading"
      @apply="emit('filter', $event)"
    />

    <ol v-if="!loading && tasks.length" class="review-list">
      <li
        v-for="task in tasks"
        :key="task.id"
        class="review-item"
        :class="{ completed: task.status === 'COMPLETED' }"
      >
        <div class="review-marker" aria-hidden="true">
          <span>第 {{ task.questionIndex + 1 }} 题</span>
        </div>

        <div class="review-body">
          <div class="review-question">
            <span>{{ task.objective }}</span>
            <h3>{{ task.question }}</h3>
          </div>

          <div class="review-gaps">
            <strong>这次遗漏</strong>
            <ul>
              <li v-for="gap in task.gaps" :key="gap">{{ gap }}</li>
            </ul>
          </div>

          <div class="review-suggestion">
            <strong>下次回答前</strong>
            <p>{{ task.improvementSuggestion }}</p>
          </div>

          <InterviewReviewScheduleControl
            v-if="task.status === 'PENDING'"
            :task-id="task.id"
            :planned-review-date="task.plannedReviewDate"
            :busy="schedulingTaskId === task.id"
            @schedule="handleSchedule"
          />

          <div class="review-task-action">
            <template v-if="task.status === 'COMPLETED'">
              <div>
                <strong>已完成</strong>
                <span v-if="task.plannedReviewDate">原计划 {{ formatPlannedDate(task.plannedReviewDate) }}</span>
              </div>
              <span>{{ formatCompletedAt(task.completedAt) }}</span>
            </template>
            <button
              v-else
              type="button"
              :disabled="Boolean(completingTaskId)"
              @click="emit('complete', task.id)"
            >
              {{ completingTaskId === task.id ? "正在记录…" : "标记完成" }}
            </button>
          </div>

          <InterviewReviewAttemptPanel
            :task-id="task.id"
            :attempts="attemptsByTaskId[task.id] ?? []"
            :loading="attemptsLoadingTaskId === '*' || attemptsLoadingTaskId === task.id"
            :submitting="submittingAttemptTaskId === task.id"
            :error="attemptError"
            :summary="historiesByTaskId.get(task.id) ?? null"
            @submit="emit('submitAttempt', task.id, $event)"
          />
        </div>

        <details class="source-ledger" :class="{ grounded: task.knowledgeGrounded }">
          <summary>{{ task.knowledgeGrounded ? `参考资料（${task.knowledgeSources.length}）` : "参考资料" }}</summary>
          <template v-if="task.knowledgeSources.length">
            <div
              v-for="source in task.knowledgeSources"
              :key="`${source.source}:${source.documentVersion}:${source.chunkOrder}`"
            >
              <span>{{ source.source }}</span>
              <p>{{ source.headingPath.length ? source.headingPath.join(" / ") : "未记录标题路径" }}</p>
            </div>
          </template>
          <p v-else>这道题没有可用的参考资料。</p>
        </details>
      </li>
    </ol>

    <div v-else-if="!loading && !error" class="review-state review-empty">
      <div>
        <strong>
          {{
            history && history.totalTaskCount
              ? "没有符合筛选条件的复习题目"
              : generated
                ? "本场没有需要复习的遗漏"
                : "还没有复习题目"
          }}
        </strong>
        <p>
          {{
            history && history.totalTaskCount
              ? "调整任务状态或计划日期后查看。"
              : generated
                ? "可以直接开始下一场训练。"
                : "重新整理本场回答后即可查看。"
          }}
        </p>
      </div>
      <button v-if="!generated && !(history && history.totalTaskCount)" type="button" @click="emit('retry')">
        整理复习题目
      </button>
    </div>
  </section>
</template>

<style scoped lang="scss">
.review-card {
  display: grid;
  gap: var(--space-lg);
  padding-block: var(--space-xl);
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.review-header {
  padding-bottom: var(--space-md);
}

.review-header > div,
.review-question,
.review-gaps,
.review-suggestion,
.source-ledger,
.source-ledger div,
.review-state > div {
  display: grid;
  gap: var(--space-xs);
}

.review-question span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

h2,
h3,
p {
  margin: 0;
}

h2 {
  font-size: var(--text-xl);
}

.review-header p {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.review-list {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.review-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(14rem, 0.46fr);
  gap: var(--space-lg);
  padding-block: var(--space-xl);
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.review-item:last-child {
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.review-marker {
  display: grid;
  align-content: start;
}

.review-marker span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-md);
  font-weight: 700;
  white-space: nowrap;
}

.review-body {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
}

.review-item.completed .review-marker span {
  color: var(--color-success);
}

.review-question h3 {
  color: var(--color-ink);
  font-size: var(--text-lg);
  letter-spacing: 0;
  line-height: 1.35;
}

.review-gaps strong,
.review-suggestion strong,
.review-state strong {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.review-gaps ul {
  display: grid;
  gap: var(--space-xs);
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.review-suggestion {
  padding: var(--space-md);
  border-radius: var(--radius-control);
  background: var(--color-accent-soft);
}

.review-task-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  min-height: 2.75rem;
}

.review-task-action > div {
  display: grid;
  gap: var(--space-2xs);
}

.review-task-action strong,
.review-task-action span {
  color: var(--color-success);
  font-size: var(--text-sm);
}

.review-task-action span {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.review-suggestion p,
.source-ledger p,
.review-state p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.65;
}

.source-ledger {
  align-content: start;
  padding: 0;
  border-top: var(--rule-thin) solid var(--color-rule);
}

.source-ledger.grounded {
  border-color: var(--color-success-rule);
}

.source-ledger summary {
  padding-block: var(--space-sm);
  color: var(--color-ink-2);
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 700;
}

.source-ledger div + div {
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.source-ledger div {
  padding-bottom: var(--space-sm);
}

.source-ledger span {
  overflow-wrap: anywhere;
  font-size: var(--text-xs);
}

.source-ledger span {
  color: var(--color-ink-2);
  font-weight: 700;
}

.review-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
  min-height: 6rem;
  padding: var(--space-lg);
  border: var(--rule-thin) dashed var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.review-error {
  border-color: var(--color-error-rule);
  background: var(--color-error-soft);
}

.review-action-error {
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-thin) solid var(--color-error-rule);
  border-radius: var(--radius-control);
  color: var(--color-error);
  background: var(--color-error-soft);
  font-size: var(--text-sm);
}

button {
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-weight: 700;
  white-space: nowrap;
}

@media (max-width: 62rem) {
  .review-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .review-marker {
    justify-items: start;
  }

}

@media (max-width: 40rem) {
  .review-card {
    padding-block: var(--space-lg);
  }

  .review-item {
    gap: var(--space-md);
  }

  .review-state {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .review-task-action {
    display: grid;
  }

  button {
    width: 100%;
  }
}
</style>
