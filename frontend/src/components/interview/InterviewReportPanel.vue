<script setup lang="ts">
import { RotateCcw } from "@lucide/vue";

import type { InterviewSessionMode, InterviewSessionReport } from "@/api/interview";

defineProps<{
  report: InterviewSessionReport;
  targetRole: string;
  mode: InterviewSessionMode;
}>();

const emit = defineEmits<{
  restart: [];
}>();
</script>

<template>
  <section class="report-card" aria-labelledby="report-title">
    <header>
      <div>
        <span>{{ mode === "PROJECT_DEEP_DIVE" ? "项目深挖已完成" : "面试已完成" }}</span>
        <h2 id="report-title">本场报告</h2>
        <p>
          {{ targetRole }} · {{ mode === "PROJECT_DEEP_DIVE" ? "项目深挖" : "模拟面试" }}
          · 共回答 {{ report.answeredQuestions }} 道题
        </p>
      </div>
      <button type="button" @click="emit('restart')">
        <RotateCcw :size="16" aria-hidden="true" />
        <span>开始新面试</span>
      </button>
    </header>

    <div class="score-board">
      <div class="report-score">
        <strong>{{ report.overallAverageScore }}</strong>
        <span>综合分</span>
      </div>
      <dl class="report-metrics">
        <div>
          <dt>技术</dt>
          <dd>{{ report.averageTechnicalScore }}</dd>
        </div>
        <div>
          <dt>完整</dt>
          <dd>{{ report.averageCompletenessScore }}</dd>
        </div>
        <div>
          <dt>表达</dt>
          <dd>{{ report.averageCommunicationScore }}</dd>
        </div>
      </dl>
    </div>

    <div class="report-lists">
      <section>
        <h3>本场表现</h3>
        <ul v-if="report.strengths.length">
          <li v-for="strength in report.strengths" :key="strength">{{ strength }}</li>
        </ul>
        <p v-else>本场暂未提取出明显优势。</p>
      </section>
      <section>
        <h3>需要补充</h3>
        <ul v-if="report.gaps.length">
          <li v-for="gap in report.gaps" :key="gap">{{ gap }}</li>
        </ul>
        <p v-else>本场没有明显重复薄弱项。</p>
      </section>
    </div>
  </section>
</template>

<style scoped lang="scss">
.report-card {
  display: grid;
  gap: var(--space-xl);
  padding: var(--space-xl);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

header,
.report-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
}

header > div {
  display: grid;
  gap: var(--space-xs);
}

header span {
  color: var(--color-success);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

h2,
h3,
p,
dl {
  margin: 0;
}

h2 {
  font-size: var(--text-xl);
}

header p {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-surface);
  font-weight: 700;
  white-space: nowrap;
}

.score-board {
  display: grid;
  grid-template-columns: minmax(10rem, 0.45fr) minmax(0, 1fr);
  overflow: hidden;
  border-radius: var(--radius-surface);
  color: var(--color-graphite-text);
  background: var(--color-graphite);
}

.report-score {
  display: grid;
  align-content: center;
  justify-items: start;
  gap: var(--space-xs);
  padding: var(--space-xl);
}

.report-score strong {
  color: var(--color-info-rule);
  font-family: var(--font-display);
  font-size: var(--text-display-s);
  letter-spacing: 0;
  line-height: 1;
}

.report-score span {
  color: var(--color-graphite-muted);
  font-weight: 700;
}

.report-score p,
.report-lists p {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.report-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: center;
}

.report-metrics div {
  display: grid;
  gap: var(--space-xs);
  padding: var(--space-xl) var(--space-lg);
}

.report-metrics div:last-child {
  border-right: 0;
}

.report-metrics dt {
  color: var(--color-graphite-muted);
  font-size: var(--text-xs);
}

.report-metrics dd {
  margin: 0;
  color: var(--color-graphite-text);
  font-family: var(--font-mono);
  font-size: var(--text-md);
  font-weight: 700;
}

.report-lists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-xl);
}

.report-lists section {
  display: grid;
  align-content: start;
  gap: var(--space-sm);
}

.report-lists h3 {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.report-lists ul {
  display: grid;
  gap: var(--space-xs);
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

@media (max-width: 40rem) {
  .report-card {
    padding: var(--space-lg) var(--space-md);
  }

  header,
  .report-lists,
  .score-board {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .report-score {
    border-right: 0;
  }

  button {
    width: 100%;
  }
}
</style>
