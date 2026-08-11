<script setup lang="ts">
import { computed, shallowRef } from "vue";
import { FileSearch, LibraryBig, MessageCircleQuestion } from "@lucide/vue";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import KnowledgeAnswerPanel from "@/components/knowledge/KnowledgeAnswerPanel.vue";
import KnowledgeCatalogPanel from "@/components/knowledge/KnowledgeCatalogPanel.vue";
import KnowledgeQuestionComposer from "@/components/knowledge/KnowledgeQuestionComposer.vue";
import KnowledgeSearchPanel from "@/components/knowledge/KnowledgeSearchPanel.vue";
import {
  KNOWLEDGE_QUESTION_MAX_LENGTH,
  useKnowledgeAnswer,
} from "@/composables/useKnowledgeAnswer";
import {
  KNOWLEDGE_SEARCH_MAX_LENGTH,
  useKnowledgeSearch,
} from "@/composables/useKnowledgeSearch";
import { useKnowledgeCatalog } from "@/composables/useKnowledgeCatalog";

type KnowledgeMode = "catalog" | "search" | "answer";

const activeMode = shallowRef<KnowledgeMode>("catalog");

const {
  question,
  submittedQuestion,
  result,
  loading,
  error,
  submit,
  clear,
} = useKnowledgeAnswer();
const {
  query,
  submittedQuery,
  result: searchResult,
  loading: searchLoading,
  error: searchError,
  search,
  clear: clearSearch,
} = useKnowledgeSearch();
const {
  result: catalogResult,
  loading: catalogLoading,
  error: catalogError,
  load: loadCatalog,
} = useKnowledgeCatalog();

const activeError = computed(() => {
  if (activeMode.value === "catalog") {
    return catalogError.value;
  }
  return activeMode.value === "answer" ? error.value : searchError.value;
});

function handleSubmit(): void {
  void submit();
}

function handleSearch(): void {
  void search();
}

function handleAskFromSearch(searchQuery: string): void {
  clear();
  question.value = searchQuery;
  activeMode.value = "answer";
}
</script>

<template>
  <WorkspaceHeader mode="answer" />

  <main id="main-content" class="answer-workspace" tabindex="-1">
    <header class="answer-heading">
      <div>
        <span>资料检索与问答</span>
        <h1>查资料或提问</h1>
      </div>
      <p>先确认当前资料范围，再浏览相关原文或发起可靠资料问答。</p>
    </header>

    <div class="mode-switch" role="group" aria-label="资料使用方式">
      <button
        type="button"
        :aria-pressed="activeMode === 'catalog'"
        :class="{ 'is-active': activeMode === 'catalog' }"
        @click="activeMode = 'catalog'"
      >
        <LibraryBig :size="17" aria-hidden="true" />
        <span>资料目录</span>
      </button>
      <button
        type="button"
        :aria-pressed="activeMode === 'search'"
        :class="{ 'is-active': activeMode === 'search' }"
        @click="activeMode = 'search'"
      >
        <FileSearch :size="17" aria-hidden="true" />
        <span>查资料</span>
      </button>
      <button
        type="button"
        :aria-pressed="activeMode === 'answer'"
        :class="{ 'is-active': activeMode === 'answer' }"
        @click="activeMode = 'answer'"
      >
        <MessageCircleQuestion :size="17" aria-hidden="true" />
        <span>问资料库</span>
      </button>
    </div>

    <p v-if="activeError" class="error-banner" role="alert">{{ activeError }}</p>

    <KnowledgeCatalogPanel
      v-if="activeMode === 'catalog'"
      :loading="catalogLoading"
      :failed="Boolean(catalogError)"
      :documents="catalogResult?.documents ?? []"
      @refresh="loadCatalog"
    />

    <div v-else-if="activeMode === 'answer'" class="answer-grid">
      <KnowledgeQuestionComposer
        v-model="question"
        :busy="loading"
        :has-result="Boolean(result)"
        :max-length="KNOWLEDGE_QUESTION_MAX_LENGTH"
        @submit="handleSubmit"
        @clear="clear"
      />
      <KnowledgeAnswerPanel
        :loading="loading"
        :question="submittedQuestion"
        :result="result"
      />
    </div>

    <div v-else class="answer-grid">
      <KnowledgeQuestionComposer
        v-model="query"
        mode="search"
        :busy="searchLoading"
        :has-result="Boolean(searchResult)"
        :max-length="KNOWLEDGE_SEARCH_MAX_LENGTH"
        @submit="handleSearch"
        @clear="clearSearch"
      />
      <KnowledgeSearchPanel
        :loading="searchLoading"
        :query="submittedQuery"
        :result="searchResult"
        @ask="handleAskFromSearch"
      />
    </div>
  </main>
</template>

<style scoped lang="scss">
.answer-workspace {
  display: grid;
  gap: var(--space-xl);
  margin-left: var(--workspace-rail);
  padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
}

.answer-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.answer-heading > div {
  display: grid;
  gap: var(--space-xs);
}

.answer-heading span {
  color: var(--color-accent-strong);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 800;
}

h1,
.answer-heading p,
.error-banner {
  margin: 0;
}

h1 {
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  letter-spacing: 0;
  line-height: 1.1;
  text-wrap: balance;
}

.answer-heading p {
  max-width: 52ch;
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.6;
  text-wrap: pretty;
}

.mode-switch {
  display: inline-flex;
  width: fit-content;
  padding: var(--space-2xs);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  background: var(--color-paper-2);
}

.mode-switch button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: 0;
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: transparent;
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 700;
}

.mode-switch button.is-active {
  color: var(--color-ink);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.mode-switch button:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.error-banner {
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-thin) solid var(--color-error-rule);
  border-radius: var(--radius-control);
  color: var(--color-error);
  background: var(--color-error-soft);
  font-size: var(--text-sm);
}

.answer-grid {
  display: grid;
  grid-template-columns: minmax(18rem, 0.62fr) minmax(0, 1.38fr);
  align-items: start;
  gap: var(--space-2xl);
}

@media (max-width: 68rem) {
  .answer-workspace {
    margin-left: 0;
  }
}

@media (max-width: 58rem) {
  .answer-heading {
    align-items: start;
    flex-direction: column;
    gap: var(--space-sm);
  }

  .answer-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-xl);
  }
}

@media (max-width: 40rem) {
  .answer-workspace {
    width: calc(100% - (var(--space-md) * 2));
    margin-inline: auto;
    padding: var(--space-lg) 0 var(--space-xl);
  }

  h1 {
    font-size: var(--text-xl);
  }

  .mode-switch {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
  }

  .mode-switch button {
    min-width: 0;
    padding-inline: var(--space-xs);
  }
}

@media (hover: hover) and (pointer: fine) {
  .mode-switch button:hover:not(.is-active) {
    color: var(--color-ink-2);
    background: var(--color-paper-3);
  }
}
</style>
