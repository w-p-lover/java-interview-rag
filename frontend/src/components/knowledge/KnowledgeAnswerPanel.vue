<script setup lang="ts">
import { BookOpenCheck, CircleAlert, LibraryBig, TriangleAlert } from "@lucide/vue";
import { computed, shallowRef } from "vue";

import type { KnowledgeAnswerClaimEvidence, KnowledgeAnswerResponse } from "@/api/knowledge";
import KnowledgeClaimEvidence from "@/components/knowledge/KnowledgeClaimEvidence.vue";
import KnowledgeSourceList from "@/components/knowledge/KnowledgeSourceList.vue";
import { sendKnowledgeAnswerFeedback } from "@/api/knowledge";

const props = defineProps<{
  loading: boolean;
  streaming: boolean;
  question: string;
  result: KnowledgeAnswerResponse | null;
  reviewTaskIds?: Readonly<Record<string, string>>;
}>();

const emit = defineEmits<{ addReview: [claim: KnowledgeAnswerClaimEvidence] }>();
const feedback = shallowRef<"HELPFUL" | "NOT_HELPFUL" | "">("");
const feedbackError = shallowRef("");
const answerStatus = computed(() => {
  if (props.result?.retrievalStrategy === "UNAVAILABLE") return "检索不可用";
  if (props.result?.answer.includes("生成超时")) return "生成超时";
  if (!props.result?.grounded && props.result?.claimEvidence.some((claim) => claim.status === "CITATION_INVALID")) return "回答引用无效";
  if (!props.result?.answer && props.result?.sources.length) return "可先阅读来源";
  return props.result?.grounded ? "资料已支撑" : "资料不足";
});
const displayAnswer = computed(() => {
  if (answerStatus.value === "回答引用无效") return "本次回答的引用未通过核对，可先阅读下方资料并继续复习。";
  return props.result?.answer || (props.result?.sources.length ? "已找到相关资料，可先阅读下方来源。" : "正在检索资料…");
});
const degradationNotice = computed(() => {
  switch (props.result?.retrievalStrategy) {
    case "SPARSE_ONLY":
      return "向量检索暂时不可用，本次已切换为关键词检索；回答仍经过资料引用校验。";
    case "DENSE_ONLY":
      return "关键词检索暂时不可用，本次仅使用向量检索；回答仍经过资料引用校验。";
    case "UNAVAILABLE":
      return "向量与关键词检索均暂时不可用，本次未调用模型。";
    default:
      return "";
  }
});

async function submitFeedback(value: "HELPFUL" | "NOT_HELPFUL"): Promise<void> {
  feedbackError.value = "";
  if (!props.question || !props.result || props.streaming) return;
  try {
    await sendKnowledgeAnswerFeedback({
      question: props.question,
      grounded: props.result.grounded,
      retrievalStrategy: props.result.retrievalStrategy,
      feedback: value,
    });
    feedback.value = value;
  } catch (error) {
    feedbackError.value = error instanceof Error ? error.message : "反馈提交失败";
  }
}
</script>

<template>
  <section id="knowledge-answer" class="answer-panel" aria-labelledby="answer-title" aria-live="polite">
    <template v-if="loading && !result">
      <header>
        <div>
          <span>正在检索资料并生成回答</span>
          <h2 id="answer-title">回答</h2>
        </div>
      </header>
      <div class="answer-skeleton" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </div>
    </template>

    <template v-else-if="result">
      <header>
        <div>
          <span>{{ question }}</span>
          <h2 id="answer-title">回答</h2>
        </div>
        <strong v-if="streaming" class="is-streaming">
          正在生成
        </strong>
        <strong v-else :class="result.grounded ? 'is-grounded' : 'is-insufficient'">
          <BookOpenCheck v-if="result.grounded" :size="17" aria-hidden="true" />
          <CircleAlert v-else :size="17" aria-hidden="true" />
          {{ answerStatus }}
        </strong>
      </header>

      <p v-if="result.degraded && !streaming" class="degradation-notice" role="status">
        <TriangleAlert :size="16" aria-hidden="true" />
        <span>{{ degradationNotice }}</span>
      </p>

      <p class="answer-content">{{ displayAnswer }}</p>
      <div v-if="result.answer && !streaming && result.retrievalStrategy !== 'UNAVAILABLE'" class="answer-feedback" aria-label="回答反馈">
        <span>这份回答对你有帮助吗？</span>
        <button type="button" :class="{ selected: feedback === 'HELPFUL' }" @click="submitFeedback('HELPFUL')">有帮助</button>
        <button type="button" :class="{ selected: feedback === 'NOT_HELPFUL' }" @click="submitFeedback('NOT_HELPFUL')">没帮助</button>
        <small v-if="feedback">感谢反馈，后续会用于改进检索。</small>
        <small v-if="feedbackError" class="feedback-error">{{ feedbackError }}</small>
      </div>
      <KnowledgeClaimEvidence :claims="result.claimEvidence" :review-task-ids="reviewTaskIds" @add-review="emit('addReview', $event)" />
      <KnowledgeSourceList :sources="result.sources" />
    </template>

    <div v-else class="answer-empty">
      <LibraryBig :size="28" aria-hidden="true" />
      <div>
        <h2 id="answer-title">回答</h2>
        <p>提出问题后，回答和实际命中的资料会显示在这里。</p>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.answer-panel {
  display: grid;
  align-content: start;
  gap: var(--space-lg);
  min-width: 0;
  min-height: 28rem;
  padding: var(--space-xs) 0 var(--space-xl);
}

header,
header > div,
.answer-empty,
header > strong {
  display: flex;
}

header {
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

header > div {
  min-width: 0;
  flex-direction: column;
  gap: var(--space-2xs);
}

header span {
  max-width: 68ch;
  overflow: hidden;
  color: var(--color-muted);
  font-size: var(--text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

h2,
p {
  margin: 0;
}

h2 {
  color: var(--color-ink);
  font-size: var(--text-xl);
}

header > strong {
  align-items: center;
  gap: var(--space-xs);
  flex: 0 0 auto;
  font-size: var(--text-sm);
}

.is-grounded {
  color: var(--color-success);
}

.is-insufficient {
  color: var(--color-warning);
}

.is-streaming {
  color: var(--color-accent);
}

.degradation-notice {
  display: flex;
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
  color: var(--color-warning);
  background: var(--color-warning-soft);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.degradation-notice svg {
  flex: 0 0 auto;
  margin-top: 0.1rem;
}

.answer-content {
  max-width: 74ch;
  color: var(--color-ink-2);
  white-space: pre-wrap;
  font-size: var(--text-base);
  line-height: 1.8;
  overflow-wrap: anywhere;
}

.answer-feedback { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-xs); color: var(--color-muted); font-size: var(--text-xs); }
.answer-feedback button { padding: var(--space-2xs) var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-round); color: var(--color-ink-2); background: transparent; font: inherit; }
.answer-feedback button.selected { border-color: var(--color-accent); color: var(--color-accent); background: var(--color-accent-soft); }
.answer-feedback small { color: var(--color-success); }
.answer-feedback .feedback-error { color: var(--color-error); }

.answer-empty {
  align-items: flex-start;
  gap: var(--space-md);
  margin-block: auto;
  padding: var(--space-xl) 0;
  color: var(--color-muted);
}

.answer-empty svg {
  flex: 0 0 auto;
  color: var(--color-accent);
}

.answer-empty > div {
  display: grid;
  gap: var(--space-xs);
}

.answer-empty h2 {
  font-size: var(--text-lg);
}

.answer-empty p {
  max-width: 48ch;
  font-size: var(--text-sm);
  line-height: 1.6;
}

.answer-skeleton {
  display: grid;
  gap: var(--space-sm);
  padding-top: var(--space-xs);
}

.answer-skeleton span {
  height: 0.875rem;
  border-radius: var(--radius-control);
  background: var(--color-paper-3);
  animation: skeleton-pulse 1.2s ease-in-out infinite alternate;
}

.answer-skeleton span:nth-child(2) {
  width: 92%;
}

.answer-skeleton span:nth-child(3) {
  width: 78%;
}

.answer-skeleton span:nth-child(4) {
  width: 56%;
}

@keyframes skeleton-pulse {
  to {
    opacity: 0.48;
  }
}

@media (max-width: 40rem) {
  .answer-panel {
    min-height: 20rem;
  }

  header {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-sm);
  }
}

@media (prefers-reduced-motion: reduce) {
  .answer-skeleton span {
    animation: none;
  }
}
</style>
