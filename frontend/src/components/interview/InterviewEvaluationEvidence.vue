<script setup lang="ts">
import { computed } from "vue";
import { BookMarked, Quote } from "@lucide/vue";

import type { InterviewAnswerEvaluation, InterviewKnowledgeEvidence } from "@/api/interview";

const props = defineProps<{
  evaluation: InterviewAnswerEvaluation;
}>();

const hasEvidence = computed(() => (
  props.evaluation.answerEvidence.length > 0
  || props.evaluation.knowledgeEvidence.length > 0
));

function knowledgeEvidenceKey(evidence: InterviewKnowledgeEvidence): string {
  return [
    evidence.source,
    evidence.documentVersion,
    evidence.chunkOrder,
    evidence.quote,
  ].join(":");
}

function sourceLabel(evidence: InterviewKnowledgeEvidence): string {
  const heading = evidence.headingPath.join(" / ");
  return heading ? `${evidence.source} · ${heading}` : evidence.source;
}
</script>

<template>
  <section v-if="hasEvidence" class="evaluation-evidence" aria-label="评分依据">
    <h3 class="evidence-title">
      <Quote :size="16" aria-hidden="true" />
      评分依据
    </h3>

    <div v-if="evaluation.answerEvidence.length" class="evidence-group">
      <strong>你的回答原文</strong>
      <blockquote
        v-for="quote in evaluation.answerEvidence"
        :key="quote"
      >
        {{ quote }}
      </blockquote>
    </div>

    <div v-if="evaluation.knowledgeEvidence.length" class="evidence-group">
      <strong>知识资料对照</strong>
      <ul class="knowledge-evidence-list">
        <li
          v-for="evidence in evaluation.knowledgeEvidence"
          :key="knowledgeEvidenceKey(evidence)"
        >
          <span class="knowledge-source">
            <BookMarked :size="14" aria-hidden="true" />
            {{ sourceLabel(evidence) }}
          </span>
          <blockquote>{{ evidence.quote }}</blockquote>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped lang="scss">
.evaluation-evidence,
.evidence-group,
.knowledge-evidence-list,
.knowledge-evidence-list li {
  display: grid;
}

.evaluation-evidence {
  gap: var(--space-md);
  min-width: 0;
  padding-top: var(--space-sm);
  color: inherit;
}

.evidence-title,
.knowledge-source {
  display: inline-flex;
  align-items: center;
}

.evidence-title {
  gap: var(--space-xs);
  margin: 0;
  color: inherit;
  font-size: var(--text-sm);
}

.evidence-title svg,
.knowledge-source svg {
  flex: 0 0 auto;
}

.evidence-group,
.knowledge-evidence-list,
.knowledge-evidence-list li {
  gap: var(--space-xs);
}

.evidence-group > strong,
.knowledge-source {
  font-size: var(--text-xs);
  opacity: 0.7;
}

.knowledge-evidence-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.knowledge-source {
  gap: var(--space-xs);
  min-width: 0;
  overflow-wrap: anywhere;
}

blockquote {
  margin: 0;
  padding-left: var(--space-md);
  border-left: 2px solid currentColor;
  color: inherit;
  font-size: var(--text-sm);
  line-height: 1.6;
  opacity: 0.82;
  overflow-wrap: anywhere;
}
</style>
