<script setup lang="ts">
import {
  AlertTriangle,
  BookOpenCheck,
  RefreshCw,
  Search,
  Target,
} from "@lucide/vue";
import { computed, shallowRef, watch } from "vue";

import type {
  ProjectDeepDiveDimension,
  ResumeEvidenceAuditResult,
  ResumeEvidenceConfirmedFact,
} from "@/api/interview";
import { useProjectDeepDive } from "@/composables/useProjectDeepDive";

const props = defineProps<{
  auditResult: ResumeEvidenceAuditResult | null;
}>();

const selectedProjectKey = shallowRef("");
const includeKnowledge = shallowRef(false);

const projectFacts = computed(() => (
  props.auditResult?.confirmedFacts.filter((fact) => fact.type === "PROJECT") ?? []
));
const projectOptions = computed(() => projectFacts.value.map((fact) => ({
  key: factKey(fact),
  fact,
})));
const selectedProject = computed(() => (
  projectOptions.value.find((option) => option.key === selectedProjectKey.value)?.fact ?? null
));

watch(
  projectOptions,
  (options) => {
    if (!options.some((option) => option.key === selectedProjectKey.value)) {
      selectedProjectKey.value = options[0]?.key ?? "";
    }
  },
  { immediate: true },
);

const {
  result,
  loading,
  error,
  canGenerate,
  isStale,
  generate,
} = useProjectDeepDive(
  () => selectedProject.value,
  () => includeKnowledge.value,
  () => props.auditResult?.requestId ?? "",
);

const dimensionMeta: Record<ProjectDeepDiveDimension, { label: string; index: string }> = {
  FOUNDATION: { label: "基础", index: "01" },
  TRADE_OFF: { label: "取舍", index: "02" },
  FAILURE: { label: "故障", index: "03" },
  EVOLUTION: { label: "扩展", index: "04" },
};

const actionLabel = computed(() => {
  if (loading.value) return "正在生成…";
  return result.value ? "重新生成" : "生成深挖题";
});

function factKey(fact: ResumeEvidenceConfirmedFact): string {
  return `${fact.type}\u0000${fact.statement}\u0000${fact.evidence}`;
}

function handleGenerate(): void {
  void generate();
}
</script>

<template>
  <section class="deep-dive-panel" aria-labelledby="project-deep-dive-title">
    <header class="deep-dive-heading">
      <div class="deep-dive-title">
        <span class="deep-dive-mark" aria-hidden="true"><Target :size="18" /></span>
        <div>
          <h3 id="project-deep-dive-title">项目深挖面试官</h3>
          <p>围绕一个已确认项目，固定覆盖基础、取舍、故障和扩展。</p>
        </div>
      </div>
      <button
        type="button"
        class="deep-dive-action"
        :disabled="!canGenerate || loading"
        @click="handleGenerate"
      >
        <RefreshCw v-if="result" :size="16" aria-hidden="true" />
        <Search v-else :size="16" aria-hidden="true" />
        <span>{{ actionLabel }}</span>
      </button>
    </header>

    <p v-if="!props.auditResult" class="deep-dive-empty">
      <BookOpenCheck :size="17" aria-hidden="true" />
      <span>完成简历证据检查后，可以从已确认项目中生成深挖题。</span>
    </p>
    <p v-else-if="projectOptions.length === 0" class="deep-dive-empty">
      <AlertTriangle :size="17" aria-hidden="true" />
      <span>当前审计结果没有确认到项目事实，请先补充项目名称与经历。</span>
    </p>

    <div v-else class="deep-dive-controls">
      <label class="project-select">
        <span>选择项目</span>
        <select v-model="selectedProjectKey">
          <option v-for="option in projectOptions" :key="option.key" :value="option.key">
            {{ option.fact.evidence }}
          </option>
        </select>
      </label>
      <label class="knowledge-toggle">
        <input v-model="includeKnowledge" type="checkbox" />
        <span>
          <strong>使用资料库补充技术背景</strong>
          <small>资料只作为答题参考，不用于补写项目经历。</small>
        </span>
      </label>
    </div>

    <div v-if="loading" class="deep-dive-loading" aria-live="polite" aria-busy="true">
      {{ includeKnowledge ? "正在生成并检索资料…" : "正在生成四类深挖题…" }}
    </div>

    <p v-if="error" class="deep-dive-error" role="alert">
      <AlertTriangle :size="16" aria-hidden="true" />
      <span>{{ error }}</span>
    </p>

    <template v-if="result && !loading">
      <p v-if="isStale" class="deep-dive-stale" role="status">
        <RefreshCw :size="15" aria-hidden="true" />
        <span>项目、审计结果或资料选项已变化；请重新生成后再参考。</span>
      </p>

      <ol class="question-list" aria-label="项目深挖题">
        <li v-for="question in result.questions" :key="question.dimension" class="question-item">
          <span class="question-index" aria-hidden="true">
            {{ dimensionMeta[question.dimension].index }}
          </span>
          <div class="question-content">
            <header class="question-heading">
              <strong>{{ dimensionMeta[question.dimension].label }}</strong>
              <span>{{ question.objective }}</span>
            </header>
            <p>{{ question.question }}</p>
            <details v-if="question.knowledgeSources.length" class="knowledge-sources">
              <summary>查看 {{ question.knowledgeSources.length }} 条资料来源</summary>
              <ul>
                <li
                  v-for="source in question.knowledgeSources"
                  :key="`${source.source}-${source.documentVersion}-${source.chunkOrder}`"
                >
                  <strong>{{ source.source }}</strong>
                  <span>{{ source.headingPath.join(" / ") || "文档正文" }}</span>
                  <p>{{ source.content }}</p>
                </li>
              </ul>
            </details>
          </div>
        </li>
      </ol>
    </template>
  </section>
</template>

<style scoped lang="scss">
.deep-dive-panel {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.deep-dive-heading,
.deep-dive-title,
.deep-dive-action,
.deep-dive-empty,
.deep-dive-error,
.deep-dive-stale,
.knowledge-toggle,
.question-heading {
  display: flex;
  align-items: center;
}

.deep-dive-heading {
  justify-content: space-between;
  gap: var(--space-md);
}

.deep-dive-title {
  align-items: flex-start;
  gap: var(--space-sm);
  min-width: 0;
}

.deep-dive-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.deep-dive-title div,
.project-select,
.knowledge-toggle span,
.question-content {
  display: grid;
  gap: var(--space-2xs);
}

.deep-dive-title h3,
.deep-dive-title p,
.deep-dive-empty,
.deep-dive-loading,
.deep-dive-error,
.deep-dive-stale,
.knowledge-toggle strong,
.knowledge-toggle small,
.question-heading span,
.question-content > p,
.knowledge-sources p {
  margin: 0;
}

.deep-dive-title h3 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.deep-dive-title p,
.deep-dive-empty,
.deep-dive-loading,
.knowledge-toggle small,
.question-heading span {
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.55;
}

.deep-dive-action {
  flex: 0 0 auto;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: transparent;
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 700;
}

.deep-dive-action:disabled {
  border-color: var(--color-rule-2);
  color: var(--color-faint);
  cursor: not-allowed;
}

.deep-dive-action:focus-visible,
.project-select select:focus-visible,
.knowledge-toggle input:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.deep-dive-empty,
.deep-dive-loading,
.deep-dive-error,
.deep-dive-stale {
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
}

.deep-dive-empty,
.deep-dive-loading {
  background: var(--color-paper-2);
}

.deep-dive-error {
  color: var(--color-error);
  background: var(--color-error-soft);
}

.deep-dive-stale {
  color: var(--color-warning);
  background: var(--color-warning-soft);
  font-size: var(--text-sm);
}

.deep-dive-controls {
  display: grid;
  grid-template-columns: minmax(12rem, 0.8fr) minmax(16rem, 1.2fr);
  gap: var(--space-md);
  align-items: end;
}

.project-select {
  color: var(--color-ink);
  font-size: var(--text-sm);
  font-weight: 600;
}

.project-select select {
  width: 100%;
  min-height: 2.75rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-paper);
}

.knowledge-toggle {
  align-items: flex-start;
  gap: var(--space-sm);
  min-height: 2.75rem;
  padding: var(--space-sm);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
  cursor: pointer;
}

.knowledge-toggle input {
  flex: 0 0 auto;
  width: 1rem;
  height: 1rem;
  margin-top: var(--space-2xs);
  accent-color: var(--color-accent);
}

.knowledge-toggle strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.question-list {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.question-item {
  display: grid;
  grid-template-columns: 2.5rem minmax(0, 1fr);
  gap: var(--space-md);
  padding-block: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.question-index {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: 700;
}

.question-content {
  gap: var(--space-sm);
  min-width: 0;
}

.question-heading {
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--space-xs) var(--space-sm);
}

.question-heading strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.question-content > p {
  max-width: 72ch;
  color: var(--color-ink-2);
  font-size: var(--text-base);
  line-height: 1.65;
  text-wrap: pretty;
}

.knowledge-sources summary {
  width: fit-content;
  color: var(--color-accent);
  cursor: pointer;
  font-size: var(--text-xs);
  font-weight: 700;
}

.knowledge-sources ul {
  display: grid;
  gap: var(--space-sm);
  margin: var(--space-sm) 0 0;
  padding: 0;
  list-style: none;
}

.knowledge-sources li {
  display: grid;
  gap: var(--space-2xs);
  padding: var(--space-sm) 0;
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.knowledge-sources strong,
.knowledge-sources span {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.knowledge-sources p {
  max-width: 72ch;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 48rem) {
  .deep-dive-heading,
  .deep-dive-controls {
    grid-template-columns: minmax(0, 1fr);
  }

  .deep-dive-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .deep-dive-action {
    width: 100%;
  }

  .question-item {
    grid-template-columns: 2rem minmax(0, 1fr);
    gap: var(--space-sm);
  }
}
</style>
