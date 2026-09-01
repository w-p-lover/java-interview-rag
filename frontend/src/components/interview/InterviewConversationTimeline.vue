<script setup lang="ts">
import {
  CheckCircle2,
  CircleDot,
  LoaderCircle,
  MessageSquareText,
  Sparkles,
} from "@lucide/vue";

import type { InterviewConversationEvent } from "@/api/interview";

defineProps<{
  events: InterviewConversationEvent[];
}>();

const eventLabels: Record<InterviewConversationEvent["type"], string> = {
  ANSWER_SUBMITTED: "已收到回答",
  EVALUATION_STARTED: "开始评价",
  EVALUATION_COMPLETED: "评价完成",
  FOLLOW_UP_GENERATED: "已准备下一题",
  SESSION_COMPLETED: "本场已完成",
};

function labelFor(type: InterviewConversationEvent["type"]): string {
  return eventLabels[type];
}

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
}
</script>

<template>
  <section class="conversation-timeline" aria-labelledby="conversation-timeline-title">
    <header>
      <div>
        <span class="eyebrow">CONVERSATION LOG</span>
        <h2 id="conversation-timeline-title">对话进度</h2>
      </div>
      <span class="event-count">{{ events.length }} 个阶段</span>
    </header>

    <ol>
      <li v-for="(event, index) in events" :key="`${index}-${event.sequence}-${event.type}`">
        <span class="event-icon" aria-hidden="true">
          <MessageSquareText v-if="event.type === 'ANSWER_SUBMITTED'" :size="15" />
          <LoaderCircle v-else-if="event.type === 'EVALUATION_STARTED'" :size="15" />
          <CheckCircle2 v-else-if="event.type === 'EVALUATION_COMPLETED'" :size="15" />
          <Sparkles v-else-if="event.type === 'FOLLOW_UP_GENERATED'" :size="15" />
          <CircleDot v-else :size="15" />
        </span>
        <div class="event-content">
          <div class="event-heading">
            <strong>{{ labelFor(event.type) }}</strong>
            <time :datetime="event.occurredAt">
              第 {{ event.questionIndex + 1 }} 题 · {{ formatTime(event.occurredAt) }}
            </time>
          </div>
          <p>{{ event.summary }}</p>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.conversation-timeline {
  display: grid;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

header,
.event-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-md);
}

header > div {
  display: grid;
  gap: var(--space-3xs);
}

.eyebrow,
.event-count,
time {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

h2,
p,
ol {
  margin: 0;
}

h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
  letter-spacing: 0;
}

ol {
  display: grid;
  gap: var(--space-sm);
  padding: 0;
  list-style: none;
}

li {
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr);
  gap: var(--space-sm);
  align-items: start;
}

.event-icon {
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-accent-ink);
  background: var(--color-accent);
}

.event-content {
  display: grid;
  gap: var(--space-3xs);
  min-width: 0;
  padding-top: var(--space-3xs);
}

.event-heading {
  align-items: center;
}

.event-heading strong {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.55;
}

@media (max-width: 40rem) {
  .conversation-timeline {
    padding: var(--space-md);
  }

  .event-heading {
    display: grid;
    gap: var(--space-3xs);
  }
}
</style>
