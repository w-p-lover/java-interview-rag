<script setup lang="ts">
import { computed, shallowRef, watch } from "vue";
import {
  CalendarClock,
  Check,
  MessageCircleQuestion,
  Search,
  Send,
  ShieldCheck,
  X,
} from "@lucide/vue";

import type { InterviewReviewTask } from "@/api/interview";
import { useInterviewReviewAssistant } from "@/composables/useInterviewReviewAssistant";

type AssistantMode = "QUERY" | "SCHEDULE";

const props = defineProps<{
  sessionId: string;
  tasks: InterviewReviewTask[];
}>();

const emit = defineEmits<{
  scheduled: [];
}>();

const mode = shallowRef<AssistantMode>("QUERY");
const queryMessage = shallowRef("");
const selectedTaskId = shallowRef("");
const plannedReviewDate = shallowRef("");

const {
  busy,
  answer,
  error,
  pendingConfirmation,
  queryTasks,
  requestSchedule,
  confirmSchedule,
  cancelConfirmation,
} = useInterviewReviewAssistant();

const pendingTasks = computed(() => props.tasks.filter((task) => task.status === "PENDING"));
const selectedTask = computed(() => (
  pendingTasks.value.find((task) => task.id === selectedTaskId.value) ?? null
));
const canRequestSchedule = computed(() => (
  Boolean(selectedTask.value && plannedReviewDate.value) && !busy.value
));

watch(
  pendingTasks,
  (tasks) => {
    if (selectedTaskId.value && !tasks.some((task) => task.id === selectedTaskId.value)) {
      selectedTaskId.value = "";
      cancelConfirmation();
    }
  },
);

watch([selectedTaskId, plannedReviewDate], () => {
  if (pendingConfirmation.value) {
    cancelConfirmation();
  }
});

function setMode(nextMode: AssistantMode): void {
  mode.value = nextMode;
  cancelConfirmation();
}

function submitQuery(): void {
  void queryTasks(props.sessionId, queryMessage.value);
}

function proposeSchedule(): void {
  if (!selectedTask.value || !plannedReviewDate.value) {
    return;
  }
  void requestSchedule(props.sessionId, selectedTask.value, plannedReviewDate.value);
}

async function confirmPendingSchedule(): Promise<void> {
  if (await confirmSchedule()) {
    emit("scheduled");
  }
}

function formatDate(value: string): string {
  const parsed = new Date(`${value}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", { dateStyle: "long" }).format(parsed);
}
</script>

<template>
  <section class="assistant-panel" aria-labelledby="review-assistant-title">
    <header class="assistant-header">
      <div class="assistant-heading">
        <MessageCircleQuestion :size="22" aria-hidden="true" />
        <div>
          <h2 id="review-assistant-title">复习助手</h2>
          <p>仅处理本场复习任务</p>
        </div>
      </div>

      <div class="assistant-modes" aria-label="复习助手模式">
        <button
          type="button"
          :aria-pressed="mode === 'QUERY'"
          :disabled="busy"
          @click="setMode('QUERY')"
        >
          <Search :size="16" aria-hidden="true" />
          查询任务
        </button>
        <button
          type="button"
          :aria-pressed="mode === 'SCHEDULE'"
          :disabled="busy"
          @click="setMode('SCHEDULE')"
        >
          <CalendarClock :size="16" aria-hidden="true" />
          调整计划
        </button>
      </div>
    </header>

    <form v-if="mode === 'QUERY'" class="assistant-query" @submit.prevent="submitQuery">
      <label class="sr-only" for="review-assistant-query">查询本场复习任务</label>
      <textarea
        id="review-assistant-query"
        v-model="queryMessage"
        rows="2"
        maxlength="1200"
        :disabled="busy"
        placeholder="例如：先练哪些待复习题？"
      />
      <button class="assistant-primary" type="submit" :disabled="busy || !queryMessage.trim()">
        <Send :size="16" aria-hidden="true" />
        {{ busy ? "正在查询…" : "发送" }}
      </button>
    </form>

    <form v-else class="assistant-schedule" @submit.prevent="proposeSchedule">
      <label class="assistant-field">
        <span>复习题目</span>
        <select v-model="selectedTaskId" :disabled="busy || !pendingTasks.length">
          <option value="">选择待复习题目</option>
          <option v-for="task in pendingTasks" :key="task.id" :value="task.id">
            第 {{ task.questionIndex + 1 }} 题 · {{ task.objective }}
          </option>
        </select>
      </label>

      <label class="assistant-field assistant-date">
        <span>计划日期</span>
        <input v-model="plannedReviewDate" type="date" :disabled="busy" />
      </label>

      <button class="assistant-primary" type="submit" :disabled="!canRequestSchedule">
        <CalendarClock :size="16" aria-hidden="true" />
        {{ busy ? "正在确认…" : "提出改期" }}
      </button>

      <p v-if="!pendingTasks.length" class="assistant-empty">
        当前列表没有待复习题目。
      </p>
    </form>

    <div v-if="pendingConfirmation" class="schedule-confirmation" aria-live="polite">
      <ShieldCheck :size="22" aria-hidden="true" />
      <div class="confirmation-copy">
        <strong>确认最终操作</strong>
        <p>
          第 {{ pendingConfirmation.questionIndex + 1 }} 题调整到
          {{ formatDate(pendingConfirmation.plannedReviewDate) }}
        </p>
        <span>{{ pendingConfirmation.question }}</span>
      </div>
      <div class="confirmation-actions">
        <button
          class="confirm-action"
          type="button"
          :disabled="busy"
          @click="confirmPendingSchedule"
        >
          <Check :size="16" aria-hidden="true" />
          {{ busy ? "正在改期…" : "确认改期" }}
        </button>
        <button
          class="cancel-action"
          type="button"
          :disabled="busy"
          title="取消改期"
          @click="cancelConfirmation"
        >
          <X :size="16" aria-hidden="true" />
          <span class="sr-only">取消改期</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="assistant-error" role="alert">{{ error }}</p>

    <div v-if="answer" class="assistant-answer" aria-live="polite">
      <strong>助手回复</strong>
      <p>{{ answer }}</p>
    </div>
  </section>
</template>

<style scoped lang="scss">
.assistant-panel {
  display: grid;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-block: var(--rule-thin) solid var(--color-rule-2);
  background: var(--color-paper-2);
}

.assistant-header,
.assistant-heading,
.assistant-modes,
.assistant-query,
.assistant-schedule,
.schedule-confirmation,
.confirmation-actions {
  display: flex;
  align-items: center;
}

.assistant-header {
  justify-content: space-between;
  gap: var(--space-lg);
}

.assistant-heading {
  gap: var(--space-sm);
  color: var(--color-accent-strong);
}

.assistant-heading > div,
.assistant-field,
.confirmation-copy,
.assistant-answer {
  display: grid;
  gap: var(--space-2xs);
}

.assistant-heading h2,
.assistant-heading p,
.confirmation-copy p,
.assistant-answer p,
.assistant-error,
.assistant-empty {
  margin: 0;
}

.assistant-heading h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
  letter-spacing: 0;
}

.assistant-heading p,
.assistant-field > span,
.assistant-empty {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.assistant-modes {
  min-height: 2.75rem;
  overflow: hidden;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-surface);
}

.assistant-modes button,
.assistant-primary,
.confirm-action,
.cancel-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  border: 0;
  font-weight: 700;
}

.assistant-modes button {
  padding-inline: var(--space-md);
  color: var(--color-muted);
  background: transparent;
}

.assistant-modes button + button {
  border-left: var(--rule-thin) solid var(--color-rule);
}

.assistant-modes button[aria-pressed="true"] {
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
}

.assistant-query,
.assistant-schedule {
  align-items: end;
  gap: var(--space-sm);
}

.assistant-query textarea,
.assistant-field select,
.assistant-field input {
  min-height: 2.75rem;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-surface);
  font: inherit;
}

.assistant-query textarea {
  flex: 1;
  min-height: 4.5rem;
  padding: var(--space-sm) var(--space-md);
  resize: vertical;
  line-height: 1.5;
}

.assistant-field {
  flex: 1 1 18rem;
}

.assistant-field > span {
  font-weight: 700;
}

.assistant-field select,
.assistant-field input {
  width: 100%;
  padding-inline: var(--space-sm);
}

.assistant-date {
  flex: 0 1 12rem;
}

.assistant-primary,
.confirm-action {
  padding-inline: var(--space-md);
  color: var(--color-accent-ink);
  background: var(--color-accent-strong);
  border-radius: var(--radius-control);
  white-space: nowrap;
}

.schedule-confirmation {
  gap: var(--space-md);
  padding: var(--space-md);
  border-block: var(--rule-thin) solid var(--color-warning-rule);
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.confirmation-copy {
  flex: 1;
  min-width: 0;
}

.confirmation-copy strong {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.confirmation-copy p,
.confirmation-copy span {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.confirmation-copy span {
  overflow-wrap: anywhere;
}

.confirmation-actions {
  gap: var(--space-xs);
}

.cancel-action {
  width: 2.75rem;
  padding: 0;
  border: var(--rule-thin) solid var(--color-warning-rule);
  border-radius: var(--radius-control);
  color: var(--color-warning);
  background: var(--color-surface);
}

.assistant-answer {
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.assistant-answer strong {
  color: var(--color-ink-2);
  font-size: var(--text-xs);
}

.assistant-answer p {
  max-width: 72ch;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.65;
  white-space: pre-wrap;
}

.assistant-error {
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-thin) solid var(--color-error-rule);
  border-radius: var(--radius-control);
  color: var(--color-error);
  background: var(--color-error-soft);
  font-size: var(--text-sm);
}

.assistant-empty {
  flex-basis: 100%;
}

button,
textarea,
select,
input {
  transition:
    border-color var(--dur-short) var(--ease-out),
    background-color var(--dur-short) var(--ease-out),
    color var(--dur-short) var(--ease-out);
}

button:not(:disabled) {
  cursor: pointer;
}

button:disabled,
textarea:disabled,
select:disabled,
input:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

button:focus-visible,
textarea:focus-visible,
select:focus-visible,
input:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: 2px;
}

.assistant-primary:not(:disabled):hover,
.confirm-action:not(:disabled):hover {
  background: var(--color-accent);
}

.assistant-modes button:not(:disabled):hover {
  color: var(--color-ink-2);
  background: var(--color-paper-3);
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

@media (max-width: 52rem) {
  .assistant-header,
  .assistant-query,
  .assistant-schedule,
  .schedule-confirmation {
    align-items: stretch;
    flex-direction: column;
  }

  .assistant-modes,
  .assistant-primary,
  .confirmation-actions {
    width: 100%;
  }

  .assistant-modes button,
  .assistant-primary,
  .confirm-action {
    flex: 1;
  }

  .assistant-field,
  .assistant-date {
    flex-basis: auto;
    width: 100%;
  }
}

@media (max-width: 40rem) {
  .assistant-panel {
    padding-inline: var(--space-md);
  }
}

@media (prefers-reduced-motion: reduce) {
  button,
  textarea,
  select,
  input {
    transition: none;
  }
}
</style>
