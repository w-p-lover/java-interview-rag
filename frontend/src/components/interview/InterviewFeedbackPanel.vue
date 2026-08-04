<script setup lang="ts">
import {
  BookOpenCheck,
  CheckCircle2,
  CircleAlert,
  Info,
  Lightbulb,
  MessageSquareText,
} from "@lucide/vue";

import type { InterviewSessionAnswer } from "@/api/interview";

defineProps<{
  answer: InterviewSessionAnswer | null;
}>();
</script>

<template>
  <aside class="feedback-panel" aria-labelledby="feedback-title">
    <header>
      <h2 id="feedback-title">上一题反馈</h2>
    </header>

    <div v-if="!answer" class="feedback-empty">
      <span class="feedback-empty__icon">
        <MessageSquareText :size="20" aria-hidden="true" />
      </span>
      <div>
        <strong>还没有反馈</strong>
        <p>提交第一题后，这里会显示评分和建议。</p>
      </div>
    </div>

    <template v-else>
      <div class="overall-score">
        <span>综合表现</span>
        <p>
          <strong>{{ answer.evaluation.overallScore }}</strong>
          <small>/ 100</small>
        </p>
      </div>

      <dl class="score-list">
        <div class="score-item score-item--technical">
          <div>
            <dt>技术准确</dt>
            <dd>{{ answer.evaluation.technicalScore }}</dd>
          </div>
          <progress
            max="100"
            :value="answer.evaluation.technicalScore"
            aria-label="技术准确得分"
          ></progress>
        </div>
        <div class="score-item score-item--completeness">
          <div>
            <dt>回答完整</dt>
            <dd>{{ answer.evaluation.completenessScore }}</dd>
          </div>
          <progress
            max="100"
            :value="answer.evaluation.completenessScore"
            aria-label="回答完整得分"
          ></progress>
        </div>
        <div class="score-item score-item--communication">
          <div>
            <dt>表达清晰</dt>
            <dd>{{ answer.evaluation.communicationScore }}</dd>
          </div>
          <progress
            max="100"
            :value="answer.evaluation.communicationScore"
            aria-label="表达清晰得分"
          ></progress>
        </div>
      </dl>

      <p :class="['evidence-note', { grounded: answer.evaluation.knowledgeGrounded }]">
        <BookOpenCheck v-if="answer.evaluation.knowledgeGrounded" :size="14" aria-hidden="true" />
        <Info v-else :size="14" aria-hidden="true" />
        {{ answer.evaluation.knowledgeGrounded ? "有资料参考" : "仅根据回答评价" }}
      </p>

      <section v-if="answer.evaluation.strengths.length" class="feedback-section feedback-section--strengths">
        <h3>
          <CheckCircle2 :size="16" aria-hidden="true" />
          做得好
        </h3>
        <ul>
          <li v-for="strength in answer.evaluation.strengths" :key="strength">{{ strength }}</li>
        </ul>
      </section>

      <section v-if="answer.evaluation.gaps.length" class="feedback-section feedback-section--gaps">
        <h3>
          <CircleAlert :size="16" aria-hidden="true" />
          需要补充
        </h3>
        <ul>
          <li v-for="gap in answer.evaluation.gaps" :key="gap">{{ gap }}</li>
        </ul>
      </section>

      <section class="suggestion">
        <h3>
          <Lightbulb :size="16" aria-hidden="true" />
          改进建议
        </h3>
        <p>{{ answer.evaluation.improvementSuggestion }}</p>
      </section>
    </template>
  </aside>
</template>

<style scoped lang="scss">
.feedback-panel {
  display: grid;
  align-content: start;
  gap: var(--space-lg);
  min-width: 0;
  padding: var(--space-xl);
  color: var(--color-graphite-text);
  background: var(--color-graphite);
}

header {
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-graphite-2);
}

h2,
h3,
p,
dl {
  margin: 0;
}

h2 {
  color: var(--color-graphite-text);
  font-size: var(--text-lg);
  letter-spacing: 0;
}

.feedback-empty {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
  padding-block: var(--space-lg);
  color: var(--color-graphite-muted);
}

.feedback-empty__icon {
  display: grid;
  flex: 0 0 2.5rem;
  width: 2.5rem;
  height: 2.5rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-accent-soft);
  background: var(--color-graphite-2);
}

.feedback-empty > div {
  display: grid;
  gap: var(--space-xs);
}

.feedback-empty strong {
  color: var(--color-graphite-text);
}

.feedback-empty p {
  color: var(--color-graphite-muted);
  font-size: var(--text-sm);
}

.overall-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.overall-score > span {
  color: var(--color-graphite-muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.overall-score p {
  display: flex;
  align-items: baseline;
  gap: var(--space-xs);
}

.overall-score strong {
  color: var(--color-graphite-text);
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  letter-spacing: 0;
  line-height: 1;
}

.overall-score small {
  color: var(--color-graphite-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.score-list {
  display: grid;
  gap: var(--space-sm);
  padding-block: var(--space-md);
  border-block: var(--rule-thin) solid var(--color-graphite-2);
}

.score-item {
  display: grid;
  gap: var(--space-xs);
}

.score-item > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.score-item dt {
  color: var(--color-graphite-muted);
  font-size: var(--text-xs);
}

.score-item dd {
  margin: 0;
  color: var(--color-graphite-text);
  font-family: var(--font-mono);
  font-weight: 700;
}

.score-item progress {
  width: 100%;
  height: var(--space-2xs);
  overflow: hidden;
  border: 0;
  border-radius: var(--radius-round);
  background: var(--color-graphite-2);
}

.score-item progress::-webkit-progress-bar {
  border-radius: inherit;
  background: var(--color-graphite-2);
}

.score-item progress::-webkit-progress-value {
  border-radius: inherit;
  background: var(--score-color);
}

.score-item progress::-moz-progress-bar {
  border-radius: inherit;
  background: var(--score-color);
}

.score-item--technical {
  --score-color: var(--color-accent);
}

.score-item--completeness {
  --score-color: var(--color-success-rule);
}

.score-item--communication {
  --score-color: var(--color-info-rule);
}

.evidence-note {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  width: fit-content;
  color: var(--color-info-rule);
  font-size: var(--text-xs);
}

.evidence-note.grounded {
  color: var(--color-success-rule);
}

section {
  display: grid;
  gap: var(--space-sm);
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-graphite-2);
}

h3 {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-graphite-text);
  font-size: var(--text-sm);
}

.feedback-section--strengths h3 svg {
  color: var(--color-success-rule);
}

.feedback-section--gaps h3 svg {
  color: var(--color-warning-rule);
}

.suggestion h3 svg {
  color: var(--color-accent);
}

ul {
  display: grid;
  gap: var(--space-xs);
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-graphite-muted);
  font-size: var(--text-sm);
}

.suggestion {
  padding: var(--space-md) 0 0;
}

.suggestion p {
  color: var(--color-graphite-muted);
  font-size: var(--text-sm);
  line-height: 1.65;
}

@media (max-width: 40rem) {
  .feedback-panel {
    padding: var(--space-lg) var(--space-md);
  }
}
</style>
