<script setup lang="ts">
import { computed } from "vue";

import type {
  InterviewReviewSessionHistory,
  InterviewSession,
} from "@/api/interview";

const props = defineProps<{
  session: InterviewSession;
  history: InterviewReviewSessionHistory | null;
}>();

const entries = computed(() => props.session.answers.map((answer) => {
  const review = props.history?.tasks.find((item) => item.questionIndex === answer.questionIndex);
  return {
    questionIndex: answer.questionIndex,
    question: props.session.questions[answer.questionIndex]?.question ?? `第 ${answer.questionIndex + 1} 题`,
    original: answer,
    attempts: review?.attempts ?? [],
  };
}));

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

function deltaLabel(delta: number): string {
  return delta > 0 ? `+${delta}` : String(delta);
}
</script>

<template>
  <section class="answer-history" aria-labelledby="answer-history-title">
    <header class="answer-history__heading">
      <div>
        <span>完整作答记录</span>
        <h2 id="answer-history-title">回答回溯</h2>
      </div>
      <small>{{ entries.length }} 道已答 · {{ history?.totalAttemptCount ?? 0 }} 次复习回答</small>
    </header>

    <div class="answer-history__questions">
      <details
        v-for="(entry, index) in entries"
        :key="entry.questionIndex"
        class="answer-history__question"
        :open="index === 0"
      >
        <summary>
          <span>第 {{ entry.questionIndex + 1 }} 题</span>
          <strong>{{ entry.question }}</strong>
          <small>
            正式 {{ entry.original.evaluation.overallScore }} 分
            <template v-if="entry.attempts.length"> · 复习 {{ entry.attempts.length }} 次</template>
          </small>
        </summary>

        <div class="answer-history__records">
          <section class="answer-history__record">
            <header>
              <div>
                <span>正式回答</span>
                <time :datetime="entry.original.answeredAt">{{ formatDate(entry.original.answeredAt) }}</time>
              </div>
              <strong>{{ entry.original.evaluation.overallScore }} 分</strong>
            </header>
            <p class="answer-history__answer">{{ entry.original.answer }}</p>
            <div class="answer-history__gaps">
              <strong>当时遗漏</strong>
              <ul v-if="entry.original.evaluation.gaps.length">
                <li v-for="gap in entry.original.evaluation.gaps" :key="gap">{{ gap }}</li>
              </ul>
              <p v-else>没有记录遗漏项。</p>
            </div>
          </section>

          <section
            v-for="attempt in entry.attempts"
            :key="attempt.id"
            class="answer-history__record answer-history__record--review"
          >
            <header>
              <div>
                <span>第 {{ attempt.attemptNumber }} 次复习回答</span>
                <time :datetime="attempt.createdAt">{{ formatDate(attempt.createdAt) }}</time>
              </div>
              <strong>
                {{ attempt.evaluation.overallScore }} 分
                <small :data-positive="attempt.scoreDeltaFromOriginal > 0">
                  较正式回答 {{ deltaLabel(attempt.scoreDeltaFromOriginal) }}
                </small>
              </strong>
            </header>
            <p class="answer-history__answer">{{ attempt.answer }}</p>
            <div class="answer-history__gaps">
              <strong>本次仍需补充</strong>
              <ul v-if="attempt.evaluation.gaps.length">
                <li v-for="gap in attempt.evaluation.gaps" :key="gap">{{ gap }}</li>
              </ul>
              <p v-else>本次没有记录新的遗漏项。</p>
            </div>
          </section>

          <p v-if="!entry.attempts.length" class="answer-history__empty">这道题还没有复习回答。</p>
        </div>
      </details>
    </div>
  </section>
</template>

<style scoped lang="scss">
.answer-history {
  display: grid;
  gap: var(--space-md);
  padding-block: var(--space-lg);
  border-block: var(--rule-thin) solid var(--color-rule);
}

.answer-history__heading,
.answer-history__heading > div,
.answer-history__questions,
.answer-history__records,
.answer-history__record,
.answer-history__record header,
.answer-history__record header > div,
.answer-history__gaps {
  display: grid;
}

.answer-history__heading {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: var(--space-md);
}

.answer-history__heading > div,
.answer-history__record header > div {
  gap: var(--space-2xs);
}

.answer-history__heading span,
.answer-history__heading small,
.answer-history__record span,
.answer-history__record time,
.answer-history__record header small {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.answer-history__heading span,
.answer-history__record span {
  font-family: var(--font-mono);
  font-weight: 700;
}

.answer-history__heading h2,
.answer-history__answer,
.answer-history__gaps p,
.answer-history__empty {
  margin: 0;
}

.answer-history__heading h2 {
  color: var(--color-ink);
  font-size: var(--text-xl);
}

.answer-history__questions {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.answer-history__question {
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.answer-history__question summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  min-height: 4rem;
  padding: var(--space-sm) var(--space-xs);
  cursor: pointer;
}

.answer-history__question summary > span,
.answer-history__question summary > small {
  color: var(--color-muted);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.answer-history__question summary > strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.answer-history__records {
  gap: var(--space-lg);
  padding: var(--space-md) var(--space-xs) var(--space-xl);
}

.answer-history__record {
  gap: var(--space-md);
  padding-left: var(--space-md);
  border-left: 3px solid var(--color-rule-2);
}

.answer-history__record--review {
  border-left-color: var(--color-accent);
}

.answer-history__record header {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: var(--space-md);
}

.answer-history__record header > strong {
  display: grid;
  justify-items: end;
  color: var(--color-ink);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

.answer-history__record header small[data-positive="true"] {
  color: var(--color-success);
}

.answer-history__answer {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.75;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.answer-history__gaps {
  gap: var(--space-xs);
}

.answer-history__gaps > strong {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.answer-history__gaps ul {
  display: grid;
  gap: var(--space-xs);
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.answer-history__gaps p,
.answer-history__empty {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

@media (max-width: 40rem) {
  .answer-history__heading,
  .answer-history__question summary,
  .answer-history__record header {
    grid-template-columns: minmax(0, 1fr);
  }

  .answer-history__question summary {
    gap: var(--space-xs);
  }

  .answer-history__record header > strong {
    justify-items: start;
  }
}
</style>
