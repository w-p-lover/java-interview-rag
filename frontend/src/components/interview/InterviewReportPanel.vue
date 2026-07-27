<script setup lang="ts">
import type { InterviewSessionReport } from "@/api/interview";

defineProps<{
  report: InterviewSessionReport;
  targetRole: string;
}>();

const emit = defineEmits<{
  restart: [];
}>();
</script>

<template>
  <section class="report-card" aria-labelledby="report-title">
    <header>
      <div>
        <span>面试已完成</span>
        <h2 id="report-title">{{ targetRole }} · 本场报告</h2>
      </div>
      <button type="button" @click="emit('restart')">开始新面试</button>
    </header>

    <div class="report-score">
      <strong>{{ report.overallAverageScore }}</strong>
      <div>
        <span>综合平均分</span>
        <p>共完成 {{ report.answeredQuestions }} 道题</p>
      </div>
    </div>

    <dl class="report-metrics">
      <div>
        <dt>技术平均</dt>
        <dd>{{ report.averageTechnicalScore }}</dd>
      </div>
      <div>
        <dt>完整平均</dt>
        <dd>{{ report.averageCompletenessScore }}</dd>
      </div>
      <div>
        <dt>表达平均</dt>
        <dd>{{ report.averageCommunicationScore }}</dd>
      </div>
    </dl>

    <div class="report-lists">
      <section>
        <h3>稳定优势</h3>
        <ul v-if="report.strengths.length">
          <li v-for="strength in report.strengths" :key="strength">{{ strength }}</li>
        </ul>
        <p v-else>本场暂未提取稳定优势。</p>
      </section>
      <section>
        <h3>优先补强</h3>
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
  border: var(--rule-thin) solid var(--color-success-rule);
  border-radius: var(--radius-surface);
  background: var(--color-success-soft);
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
  letter-spacing: -0.03em;
}

button {
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-success-rule);
  border-radius: var(--radius-control);
  color: var(--color-success);
  background: var(--color-surface);
  font-weight: 700;
  white-space: nowrap;
}

.report-score {
  justify-content: start;
}

.report-score strong {
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-display-s);
  letter-spacing: -0.07em;
  line-height: 1;
}

.report-score div {
  display: grid;
  gap: var(--space-2xs);
}

.report-score span {
  color: var(--color-ink-2);
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
  border-block: var(--rule-thin) solid var(--color-success-rule);
}

.report-metrics div {
  display: grid;
  gap: var(--space-xs);
  padding-block: var(--space-md);
}

.report-metrics dt {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.report-metrics dd {
  margin: 0;
  color: var(--color-ink);
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
  .report-lists {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  button {
    width: 100%;
  }
}
</style>
