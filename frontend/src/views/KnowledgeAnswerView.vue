<script setup lang="ts">
import { computed, nextTick, onMounted, shallowRef } from "vue";
import { FileSearch, LibraryBig, MessageCircleQuestion } from "@lucide/vue";
import { useRoute } from "vue-router";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import KnowledgeAnswerPanel from "@/components/knowledge/KnowledgeAnswerPanel.vue";
import KnowledgeCatalogPanel from "@/components/knowledge/KnowledgeCatalogPanel.vue";
import KnowledgeDocumentDetailPanel from "@/components/knowledge/KnowledgeDocumentDetailPanel.vue";
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
import { completeKnowledgeReviewTask, createKnowledgeReviewTask, getKnowledgeReviewAttempts, getKnowledgeReviewPath, getKnowledgeReviewTasks, scheduleKnowledgeReviewTask, submitKnowledgeReviewAttempt, type KnowledgeAnswerClaimEvidence, type KnowledgeReviewAttempt, type KnowledgeReviewPathResult, type KnowledgeReviewTask } from "@/api/knowledge";
import KnowledgeReviewTaskList from "@/components/knowledge/KnowledgeReviewTaskList.vue";

type KnowledgeMode = "catalog" | "search" | "answer";

const route = useRoute();
const highlightedReviewTaskId = computed(() => {
  const taskId = typeof route.query.reviewTaskId === "string" ? route.query.reviewTaskId.trim() : "";
  return taskId.length <= 100 ? taskId : "";
});
const initialMode = route.query.mode;
const activeMode = shallowRef<KnowledgeMode>(
  initialMode === "search" || initialMode === "answer" ? initialMode : "catalog",
);
const reviewTasks = shallowRef<KnowledgeReviewTask[]>([]);
const reviewAttempts = shallowRef<Record<string, KnowledgeReviewAttempt[]>>({});
const reviewError = shallowRef("");
const reviewPath = shallowRef<KnowledgeReviewPathResult | null>(null);

const {
  question,
  submittedQuestion,
  result,
  loading,
  streaming,
  error,
  submit,
  clear,
  cancel,
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
  selectedSource,
  details: documentDetails,
  detailsLoading,
  detailsError,
  deleting: documentDeleting,
  rebuilding: documentRebuilding,
  rebuildError,
  notice: catalogNotice,
  refresh: refreshCatalog,
  selectDocument,
  closeDetails,
  retryDetails,
  deleteSelectedDocument,
  rebuildSelectedDocument,
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

async function handleSearchFromOutline(searchQuery: string): Promise<void> {
  clearSearch();
  query.value = searchQuery;
  activeMode.value = "search";
  await nextTick();
  document.getElementById("knowledge-question-input")?.focus();
}

async function handleAddReview(claim: KnowledgeAnswerClaimEvidence): Promise<void> {
  if (!submittedQuestion.value) return;
  reviewError.value = "";
  try {
    const task = await createKnowledgeReviewTask(submittedQuestion.value, claim);
    reviewTasks.value = [task, ...reviewTasks.value.filter((item) => item.id !== task.id)];
  } catch (error) {
    reviewError.value = error instanceof Error ? error.message : "加入复习失败";
  }
}

async function handleCompleteReview(task: KnowledgeReviewTask): Promise<void> {
  try {
    const updated = await completeKnowledgeReviewTask(task.id);
    reviewTasks.value = reviewTasks.value.map((item) => item.id === task.id ? updated : item);
    reviewPath.value = await getKnowledgeReviewPath();
  }
  catch (error) { reviewError.value = error instanceof Error ? error.message : "完成复习失败"; }
}

async function handleScheduleReview(task: KnowledgeReviewTask, event: Event): Promise<void> {
  const date = (event.target as HTMLInputElement).value || null;
  try {
    const updated = await scheduleKnowledgeReviewTask(task.id, date);
    reviewTasks.value = reviewTasks.value.map((item) => item.id === task.id ? updated : item);
    reviewPath.value = await getKnowledgeReviewPath();
  }
  catch (error) { reviewError.value = error instanceof Error ? error.message : "安排复习失败"; }
}

async function handleSubmitReview(task: KnowledgeReviewTask, answer: string): Promise<void> {
  try {
    await submitKnowledgeReviewAttempt(task.id, answer);
    reviewAttempts.value = { ...reviewAttempts.value, [task.id]: await getKnowledgeReviewAttempts(task.id) };
    reviewPath.value = await getKnowledgeReviewPath();
    reviewError.value = "复习回答已提交并完成评分。";
  } catch (error) { reviewError.value = error instanceof Error ? error.message : "复习评分失败"; }
}

onMounted(async () => {
  try {
    reviewTasks.value = await getKnowledgeReviewTasks();
    reviewPath.value = await getKnowledgeReviewPath();
    await nextTick();
    focusReviewTask(highlightedReviewTaskId.value);
  }
  catch (error) { reviewError.value = error instanceof Error ? error.message : "复习任务加载失败"; }
});

function focusReviewTask(taskId: string): void {
  if (!taskId) {
    return;
  }
  const target = document.getElementById(`knowledge-review-task-${taskId}`)
    ?? document.getElementById("knowledge-review-tasks");
  target?.scrollIntoView({ behavior: "smooth", block: "start" });
  target?.focus({ preventScroll: true });
}

const reviewTaskIds = computed(() => Object.fromEntries(reviewTasks.value.map((task) => [task.claim, task.id])));
</script>

<template>
  <WorkspaceHeader mode="answer" />

  <main id="main-content" class="answer-workspace" tabindex="-1">
    <header class="answer-heading">
      <h1>知识问答</h1>
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
    <p v-if="reviewError" class="error-banner" role="alert">{{ reviewError }}</p>

    <KnowledgeReviewTaskList
      :tasks="reviewTasks"
      :attempts-by-task-id="reviewAttempts"
      :review-path="reviewPath"
      :highlighted-task-id="highlightedReviewTaskId"
      @complete="handleCompleteReview"
      @schedule="handleScheduleReview"
      @submit="handleSubmitReview"
    />

    <div v-if="activeMode === 'catalog'" class="catalog-workspace">
      <KnowledgeCatalogPanel
        :loading="catalogLoading"
        :failed="Boolean(catalogError)"
        :documents="catalogResult?.documents ?? []"
        :selected-source="selectedSource"
        :notice="catalogNotice"
        :busy="documentDeleting || documentRebuilding"
        @refresh="refreshCatalog"
        @select="selectDocument"
      />
      <KnowledgeDocumentDetailPanel
        v-if="selectedSource"
        :source="selectedSource"
        :details="documentDetails"
        :loading="detailsLoading"
        :error="detailsError"
        :deleting="documentDeleting"
        :rebuilding="documentRebuilding"
        :rebuild-error="rebuildError"
        @close="closeDetails"
        @retry="retryDetails"
        @retire="deleteSelectedDocument"
        @rebuild="rebuildSelectedDocument"
        @search="handleSearchFromOutline"
      />
      <section v-else class="detail-placeholder" aria-labelledby="detail-placeholder-title">
        <LibraryBig :size="28" aria-hidden="true" />
        <h2 id="detail-placeholder-title">选择资料查看详情</h2>
      </section>
    </div>

    <div v-else-if="activeMode === 'answer'" class="answer-grid">
      <KnowledgeQuestionComposer
        v-model="question"
        :busy="loading"
        :has-result="Boolean(result)"
        :max-length="KNOWLEDGE_QUESTION_MAX_LENGTH"
        @submit="handleSubmit"
        @clear="clear"
        @cancel="cancel"
      />
      <KnowledgeAnswerPanel
        :loading="loading"
        :streaming="streaming"
        :question="submittedQuestion"
        :result="result"
        :review-task-ids="reviewTaskIds"
        @add-review="handleAddReview"
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
  margin-left: var(--workspace-rail);
  padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
}

.answer-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-xl);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-lg);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

h1,
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

.mode-switch {
  display: inline-flex;
  width: fit-content;
  margin-bottom: var(--space-md);
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
  margin-bottom: var(--space-md);
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


.catalog-workspace {
  display: grid;
  grid-template-columns: 18.5rem minmax(0, 1fr);
  align-items: start;
  gap: var(--space-lg);
  min-width: 0;
}

.detail-placeholder {
  display: flex;
  min-height: 24rem;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  padding: var(--space-xl);
  border-block: var(--rule-thin) solid var(--color-rule-2);
  color: var(--color-muted);
}

.detail-placeholder > div {
  display: grid;
  gap: var(--space-xs);
}

.detail-placeholder h2,
.detail-placeholder p {
  margin: 0;
}

.detail-placeholder h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
}

.detail-placeholder p {
  max-width: 42ch;
  font-size: var(--text-sm);
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

  .catalog-workspace {
    grid-template-columns: minmax(0, 1fr);
  }

  .detail-placeholder {
    min-height: 16rem;
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
