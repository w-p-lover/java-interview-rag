<script setup lang="ts">
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import KnowledgeAnswerPanel from "@/components/knowledge/KnowledgeAnswerPanel.vue";
import KnowledgeQuestionComposer from "@/components/knowledge/KnowledgeQuestionComposer.vue";
import {
  KNOWLEDGE_QUESTION_MAX_LENGTH,
  useKnowledgeAnswer,
} from "@/composables/useKnowledgeAnswer";

const {
  question,
  submittedQuestion,
  result,
  loading,
  error,
  submit,
  clear,
} = useKnowledgeAnswer();

function handleSubmit(): void {
  void submit();
}
</script>

<template>
  <WorkspaceHeader mode="answer" />

  <main id="main-content" class="answer-workspace" tabindex="-1">
    <header class="answer-heading">
      <div>
        <span>知识问答</span>
        <h1>向资料库提问</h1>
      </div>
      <p>回答仅采用已导入且达到可靠阈值的资料，并保留实际命中的原文依据。</p>
    </header>

    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>

    <div class="answer-grid">
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
}
</style>
