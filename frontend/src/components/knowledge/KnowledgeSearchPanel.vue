<script setup lang="ts">
import { FileSearch, SearchX } from "@lucide/vue";

import type { KnowledgeSearchResponse } from "@/api/knowledge";
import KnowledgeSourceList from "@/components/knowledge/KnowledgeSourceList.vue";

defineProps<{
  loading: boolean;
  query: string;
  result: KnowledgeSearchResponse | null;
}>();
</script>

<template>
  <section id="knowledge-answer" class="search-panel" aria-labelledby="search-result-title" aria-live="polite">
    <template v-if="loading">
      <header>
        <div>
          <span>正在匹配“{{ query }}”</span>
          <h2 id="search-result-title">相关资料</h2>
        </div>
      </header>
      <div class="search-skeleton" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </template>

    <template v-else-if="result?.results.length">
      <header>
        <div>
          <span>{{ result.query }}</span>
          <h2 id="search-result-title">相关资料</h2>
        </div>
        <strong>{{ result.resultCount }} 条匹配</strong>
      </header>
      <KnowledgeSourceList title="检索结果" :sources="result.results" />
    </template>

    <div v-else-if="result" class="search-empty">
      <SearchX :size="28" aria-hidden="true" />
      <div>
        <h2 id="search-result-title">没有匹配资料</h2>
        <p>当前资料库没有返回与“{{ result.query }}”相关的内容。</p>
      </div>
    </div>

    <div v-else class="search-empty">
      <FileSearch :size="28" aria-hidden="true" />
      <div>
        <h2 id="search-result-title">相关资料</h2>
        <p>匹配的资料原文会显示在这里。</p>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.search-panel {
  display: grid;
  align-content: start;
  gap: var(--space-lg);
  min-width: 0;
  min-height: 28rem;
  padding: var(--space-xs) 0 var(--space-xl);
}

header,
header > div,
.search-empty {
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

header strong {
  flex: 0 0 auto;
  color: var(--color-accent-strong);
  font-size: var(--text-sm);
}

.search-empty {
  align-items: flex-start;
  gap: var(--space-md);
  margin-block: auto;
  padding: var(--space-xl) 0;
  color: var(--color-muted);
}

.search-empty svg {
  flex: 0 0 auto;
  color: var(--color-accent);
}

.search-empty > div {
  display: grid;
  gap: var(--space-xs);
}

.search-empty h2 {
  font-size: var(--text-lg);
}

.search-empty p {
  max-width: 52ch;
  font-size: var(--text-sm);
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.search-skeleton {
  display: grid;
  gap: var(--space-sm);
  padding-top: var(--space-xs);
}

.search-skeleton span {
  height: 4.25rem;
  border-radius: var(--radius-control);
  background: var(--color-paper-3);
  animation: skeleton-pulse 1.2s ease-in-out infinite alternate;
}

.search-skeleton span:nth-child(2) {
  opacity: 0.78;
}

.search-skeleton span:nth-child(3) {
  opacity: 0.56;
}

@keyframes skeleton-pulse {
  to {
    opacity: 0.42;
  }
}

@media (max-width: 40rem) {
  .search-panel {
    min-height: 20rem;
  }

  header {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-sm);
  }
}

@media (prefers-reduced-motion: reduce) {
  .search-skeleton span {
    animation: none;
  }
}
</style>
