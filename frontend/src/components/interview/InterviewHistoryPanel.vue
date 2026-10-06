<script setup lang="ts">
import { ArrowRight, CheckCircle2, CircleDot, Clock3 } from "@lucide/vue";

import type { InterviewSessionSummary } from "@/api/interview";

defineProps<{
  sessions: InterviewSessionSummary[];
  loading: boolean;
  error: string;
  restoringSessionId: string;
}>();

const emit = defineEmits<{
  restore: [sessionId: string];
}>();

function formatDate(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(parsed);
}

function progressLabel(session: InterviewSessionSummary): string {
  if (session.status === "COMPLETED") {
    return `已完成 · ${session.questionCount} 道`;
  }
  return `已回答 ${session.answeredQuestionCount} / ${session.questionCount} 道`;
}
</script>

<template>
  <section class="history" aria-labelledby="interview-history-title">
    <header class="history__heading">
      <div>
        <span>服务端记录</span>
        <h2 id="interview-history-title">历史面试</h2>
      </div>
      <small>最近 20 场</small>
    </header>

    <p v-if="loading" class="history__state" aria-live="polite">正在读取历史面试…</p>
    <p v-else-if="error" class="history__error" role="alert">{{ error }}</p>
    <p v-else-if="!sessions.length" class="history__state">还没有已保存的面试。</p>

    <ol v-else class="history__list">
      <li v-for="session in sessions" :key="session.id">
        <div class="history__status" aria-hidden="true">
          <CheckCircle2 v-if="session.status === 'COMPLETED'" :size="18" />
          <CircleDot v-else :size="18" />
        </div>
        <div class="history__copy">
          <strong>
            {{ session.mode === "PROJECT_DEEP_DIVE" ? "项目深挖" : "模拟面试" }} · {{ session.targetRole }}
          </strong>
          <span>{{ progressLabel(session) }} · 会话 {{ session.id.slice(0, 8) }}</span>
        </div>
        <div class="history__result">
          <strong v-if="session.overallAverageScore != null">{{ session.overallAverageScore }} 分</strong>
          <span v-else>尚未生成报告</span>
          <time :datetime="session.updatedAt">
            <Clock3 :size="14" aria-hidden="true" />{{ formatDate(session.updatedAt) }}
          </time>
        </div>
        <button
          type="button"
          :disabled="Boolean(restoringSessionId)"
          :aria-label="`${session.status === 'COMPLETED' ? '查看' : '继续'} ${session.targetRole}`"
          @click="emit('restore', session.id)"
        >
          <span>{{ restoringSessionId === session.id ? "正在打开" : session.status === "COMPLETED" ? "查看" : "继续" }}</span>
          <ArrowRight :size="16" aria-hidden="true" />
        </button>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.history {
  display: grid;
  gap: var(--space-md);
  padding-top: var(--space-xl);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.history__heading,
.history__heading > div,
.history__list,
.history__copy,
.history__result {
  display: grid;
}

.history__heading {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: var(--space-md);
}

.history__heading > div,
.history__copy,
.history__result {
  gap: var(--space-2xs);
}

.history__heading span,
.history__heading small,
.history__copy span,
.history__result span,
time {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.history__heading span {
  font-family: var(--font-mono);
  font-weight: 700;
}

.history__heading h2,
.history__state,
.history__error {
  margin: 0;
}

.history__heading h2 {
  color: var(--color-ink);
  font-size: var(--text-xl);
}

.history__list {
  margin: 0;
  padding: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
  list-style: none;
}

.history__list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(9rem, auto) auto;
  align-items: center;
  gap: var(--space-md);
  min-height: 4.75rem;
  padding: var(--space-sm) var(--space-md);
}

.history__list li + li {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.history__status {
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  color: var(--color-accent);
}

.history__copy {
  min-width: 0;
}

.history__copy strong {
  overflow: hidden;
  color: var(--color-ink);
  font-size: var(--text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history__result {
  justify-items: end;
}

.history__result strong {
  color: var(--color-ink-2);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

time,
button {
  display: inline-flex;
  align-items: center;
}

time {
  gap: var(--space-xs);
  white-space: nowrap;
}

button {
  justify-content: center;
  gap: var(--space-xs);
  min-width: 5.5rem;
  min-height: 2.5rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-weight: 700;
  white-space: nowrap;
}

.history__state,
.history__error {
  padding-block: var(--space-sm);
  font-size: var(--text-sm);
}

.history__state { color: var(--color-muted); }
.history__error { color: var(--color-error); }

@media (hover: hover) and (pointer: fine) {
  button:hover:not(:disabled) { background: var(--color-accent-soft); }
}

@media (max-width: 44rem) {
  .history__list li {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .history__result {
    grid-column: 2 / -1;
    grid-row: 2;
    justify-items: start;
  }

  button {
    grid-column: 3;
    grid-row: 1;
  }
}
</style>
