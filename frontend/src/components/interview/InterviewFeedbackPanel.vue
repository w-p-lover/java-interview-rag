<script setup lang="ts">
import { BookOpenCheck, Info } from "@lucide/vue";

import type { InterviewSessionAnswer } from "@/api/interview";

defineProps<{
  answer: InterviewSessionAnswer | null;
}>();
</script>

<template>
  <aside class="feedback-card" aria-labelledby="feedback-title">
    <header>
      <h2 id="feedback-title">上一题反馈</h2>
    </header>

    <div v-if="!answer" class="feedback-empty">
      <strong>还没有反馈</strong>
      <p>提交第一题后，这里会显示评分和建议。</p>
    </div>

    <template v-else>
      <div class="overall-score">
        <strong>{{ answer.evaluation.overallScore }}</strong>
        <span>综合分 / 100</span>
      </div>

      <dl class="score-grid">
        <div>
          <dt>技术</dt>
          <dd>{{ answer.evaluation.technicalScore }}</dd>
        </div>
        <div>
          <dt>完整</dt>
          <dd>{{ answer.evaluation.completenessScore }}</dd>
        </div>
        <div>
          <dt>表达</dt>
          <dd>{{ answer.evaluation.communicationScore }}</dd>
        </div>
      </dl>

      <p :class="['evidence-note', { grounded: answer.evaluation.knowledgeGrounded }]">
        <BookOpenCheck v-if="answer.evaluation.knowledgeGrounded" :size="14" aria-hidden="true" />
        <Info v-else :size="14" aria-hidden="true" />
        {{ answer.evaluation.knowledgeGrounded ? "有资料参考" : "仅根据回答评价" }}
      </p>

      <section v-if="answer.evaluation.strengths.length">
        <h3>做得好</h3>
        <ul>
          <li v-for="strength in answer.evaluation.strengths" :key="strength">{{ strength }}</li>
        </ul>
      </section>

      <section v-if="answer.evaluation.gaps.length">
        <h3>需要补充</h3>
        <ul>
          <li v-for="gap in answer.evaluation.gaps" :key="gap">{{ gap }}</li>
        </ul>
      </section>

      <section class="suggestion">
        <h3>改进建议</h3>
        <p>{{ answer.evaluation.improvementSuggestion }}</p>
      </section>
    </template>
  </aside>
</template>

<style scoped lang="scss">
.feedback-card {
  display: grid;
  align-content: start;
  gap: var(--space-lg);
  min-width: 0;
  padding: var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-surface);
  background: var(--color-paper-2);
}

header {
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

h2,
h3,
p,
dl {
  margin: 0;
}

h2 {
  font-size: var(--text-lg);
  letter-spacing: 0;
}

.feedback-empty {
  display: grid;
  gap: var(--space-xs);
  padding-block: var(--space-lg);
  color: var(--color-muted);
}

.feedback-empty strong {
  color: var(--color-ink-2);
}

.overall-score {
  display: flex;
  align-items: end;
  gap: var(--space-sm);
}

.overall-score strong {
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-3xl);
  letter-spacing: 0;
  line-height: 1;
}

.overall-score span {
  padding-bottom: var(--space-2xs);
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-block: var(--rule-thin) solid var(--color-rule-2);
}

.score-grid div {
  display: grid;
  gap: var(--space-2xs);
  padding-block: var(--space-sm);
}

.score-grid dt {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.score-grid dd {
  margin: 0;
  color: var(--color-ink);
  font-family: var(--font-mono);
  font-weight: 700;
}

.evidence-note {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  width: fit-content;
  padding: var(--space-sm);
  border: var(--rule-thin) solid var(--color-info-rule);
  border-radius: var(--radius-control);
  color: var(--color-info);
  background: var(--color-info-soft);
  font-size: var(--text-xs);
}

.evidence-note.grounded {
  border-color: var(--color-success-rule);
  color: var(--color-success);
  background: var(--color-success-soft);
}

section {
  display: grid;
  gap: var(--space-sm);
}

h3 {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

ul {
  display: grid;
  gap: var(--space-xs);
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.suggestion {
  padding: var(--space-md);
  border-left: 0.1875rem solid var(--color-accent);
  background: var(--color-accent-soft);
}

.suggestion p {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.65;
}

@media (max-width: 40rem) {
  .feedback-card {
    padding: var(--space-lg) var(--space-md);
  }
}
</style>
