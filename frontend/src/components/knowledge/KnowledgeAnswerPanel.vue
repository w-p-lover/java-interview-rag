<script setup lang="ts">
import { BookOpenCheck, CircleAlert, LibraryBig } from "@lucide/vue";

import type { KnowledgeAnswerResponse } from "@/api/knowledge";
import KnowledgeSourceList from "@/components/knowledge/KnowledgeSourceList.vue";

defineProps<{
  loading: boolean;
  streaming: boolean;
  question: string;
  result: KnowledgeAnswerResponse | null;
}>();
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
          {{ result.grounded ? "资料已支撑" : "资料不足" }}
        </strong>
      </header>

      <p class="answer-content">{{ result.answer }}</p>
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

.answer-content {
  max-width: 74ch;
  color: var(--color-ink-2);
  white-space: pre-wrap;
  font-size: var(--text-base);
  line-height: 1.8;
  overflow-wrap: anywhere;
}

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
