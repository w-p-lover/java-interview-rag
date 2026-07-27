<script setup lang="ts">
import type { InterviewSessionAnswer } from "@/api/interview";

defineProps<{
  answer: InterviewSessionAnswer | null;
}>();
</script>

<template>
  <aside class="feedback-card" aria-labelledby="feedback-title">
    <header>
      <span>02 / 即时反馈</span>
      <h2 id="feedback-title">本题评价</h2>
    </header>

    <div v-if="!answer" class="feedback-empty">
      <strong>等待第一份回答</strong>
      <p>提交后会看到技术准确性、完整性、表达质量和具体改进建议。</p>
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
        {{
          answer.evaluation.knowledgeGrounded
            ? "本题评价使用了真实知识库资料。"
            : "本题没有可靠知识来源，技术事实未被标记为已验证。"
        }}
      </p>

      <section v-if="answer.evaluation.strengths.length">
        <h3>做得好的地方</h3>
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
        <h3>下一次怎么答</h3>
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
  display: grid;
  gap: var(--space-xs);
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

header span {
  color: var(--color-accent);
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
  font-size: var(--text-lg);
  letter-spacing: -0.025em;
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
  letter-spacing: -0.06em;
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
  padding: var(--space-sm);
  border: var(--rule-thin) solid var(--color-warning-rule);
  border-radius: var(--radius-control);
  color: var(--color-warning);
  background: var(--color-warning-soft);
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
