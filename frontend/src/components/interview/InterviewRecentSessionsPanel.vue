<script setup lang="ts">
import { ArrowRight, CheckCircle2, CircleDot, Clock3 } from "@lucide/vue";

import type { RecentInterviewSession } from "@/composables/useInterviewSession";

defineProps<{
  sessions: RecentInterviewSession[];
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

function progressLabel(session: RecentInterviewSession): string {
  if (session.status === "COMPLETED") {
    return `已完成 · ${session.questionCount} 道`;
  }
  return `进行到第 ${session.currentQuestionIndex + 1} / ${session.questionCount} 题`;
}
</script>

<template>
  <section
    v-if="loading || error || sessions.length"
    class="recent-sessions"
    aria-labelledby="recent-sessions-title"
  >
    <header class="recent-sessions__heading">
      <div>
        <span>当前浏览器</span>
        <h2 id="recent-sessions-title">最近面试</h2>
      </div>
      <small>最多保留 8 场入口</small>
    </header>

    <p v-if="loading" class="recent-sessions__state" aria-live="polite">
      正在读取最近面试…
    </p>
    <p v-if="error" class="recent-sessions__error" role="alert">{{ error }}</p>

    <ol v-if="sessions.length" class="recent-sessions__list">
      <li v-for="session in sessions" :key="session.id">
        <div class="recent-sessions__status" aria-hidden="true">
          <CheckCircle2 v-if="session.status === 'COMPLETED'" :size="18" />
          <CircleDot v-else :size="18" />
        </div>
        <div class="recent-sessions__copy">
          <strong>
            {{ session.mode === "PROJECT_DEEP_DIVE" ? "项目深挖" : "模拟面试" }} · {{ session.targetRole }}
          </strong>
          <span>{{ progressLabel(session) }}</span>
        </div>
        <time :datetime="session.updatedAt">
          <Clock3 :size="14" aria-hidden="true" />
          {{ formatDate(session.updatedAt) }}
        </time>
        <button
          type="button"
          :disabled="Boolean(restoringSessionId)"
          :aria-label="`${session.status === 'COMPLETED' ? '查看' : '继续'} ${session.targetRole}`"
          @click="emit('restore', session.id)"
        >
          <span>
            {{ restoringSessionId === session.id
              ? "正在打开"
              : session.status === "COMPLETED" ? "查看" : "继续" }}
          </span>
          <ArrowRight :size="16" aria-hidden="true" />
        </button>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.recent-sessions {
  display: grid;
  gap: var(--space-md);
  padding-top: var(--space-xl);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.recent-sessions__heading,
.recent-sessions__heading > div,
.recent-sessions__list,
.recent-sessions__copy {
  display: grid;
}

.recent-sessions__heading {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: var(--space-md);
}

.recent-sessions__heading > div {
  gap: var(--space-2xs);
}

.recent-sessions__heading span,
.recent-sessions__heading small,
.recent-sessions__copy span,
time {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.recent-sessions__heading span {
  font-family: var(--font-mono);
  font-weight: 700;
}

.recent-sessions__heading h2,
.recent-sessions__state,
.recent-sessions__error {
  margin: 0;
}

.recent-sessions__heading h2 {
  color: var(--color-ink);
  font-size: var(--text-xl);
}

.recent-sessions__list {
  margin: 0;
  padding: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
  list-style: none;
}

.recent-sessions__list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(8rem, auto) auto;
  align-items: center;
  gap: var(--space-md);
  min-height: 4.5rem;
  padding: var(--space-sm) var(--space-md);
}

.recent-sessions__list li + li {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.recent-sessions__status {
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  color: var(--color-accent);
}

.recent-sessions__copy {
  gap: var(--space-2xs);
  min-width: 0;
}

.recent-sessions__copy strong {
  overflow: hidden;
  color: var(--color-ink);
  font-size: var(--text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
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

.recent-sessions__state,
.recent-sessions__error {
  padding-block: var(--space-sm);
  font-size: var(--text-sm);
}

.recent-sessions__state {
  color: var(--color-muted);
}

.recent-sessions__error {
  color: var(--color-error);
}

@media (max-width: 40rem) {
  .recent-sessions__heading {
    grid-template-columns: minmax(0, 1fr);
  }

  .recent-sessions__list li {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  time {
    grid-column: 2 / -1;
    grid-row: 2;
  }

  button {
    grid-column: 3;
    grid-row: 1;
  }
}

@media (hover: hover) and (pointer: fine) {
  button:hover:not(:disabled) {
    background: var(--color-accent-soft);
  }
}
</style>
