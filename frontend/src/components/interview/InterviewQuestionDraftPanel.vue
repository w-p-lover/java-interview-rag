<script setup lang="ts">
import {
  BookOpenCheck,
  Check,
  RefreshCw,
  Trash2,
  X,
} from "@lucide/vue";
import { computed, type DeepReadonly } from "vue";

import type {
  InterviewFocusSource,
  InterviewPreparationResult,
  InterviewQuestion,
  InterviewQuestionGenerationResult,
} from "@/api/interview";

const props = defineProps<{
  preparation: DeepReadonly<InterviewPreparationResult>;
  generation: DeepReadonly<InterviewQuestionGenerationResult>;
  busy: boolean;
  canConfirm: boolean;
}>();

const emit = defineEmits<{
  updateQuestion: [index: number, value: string];
  updateObjective: [index: number, value: string];
  removeQuestion: [index: number];
  regenerate: [];
  cancel: [];
  confirm: [];
}>();

const canRemove = computed(() => props.generation.questions.length > 3);

const sourceLabels: Record<InterviewFocusSource, string> = {
  TARGET_ROLE: "目标岗位",
  RESUME: "简历",
  JOB_DESCRIPTION: "岗位描述",
};

function knowledgeSourceLabel(question: DeepReadonly<InterviewQuestion>): string {
  if (!question.knowledgeGrounded || !question.knowledgeSources.length) {
    return "未绑定知识资料";
  }
  return question.knowledgeSources
    .map((source) => source.headingPath.at(-1) || source.source)
    .filter((value, index, values) => values.indexOf(value) === index)
    .slice(0, 2)
    .join(" / ");
}
</script>

<template>
  <section class="draft" aria-labelledby="question-draft-title">
    <header class="draft-heading">
      <div>
        <span class="draft-kicker">Question draft</span>
        <h2 id="question-draft-title">确认本场题单</h2>
      </div>
      <span class="question-count">{{ generation.questions.length }} 道</span>
    </header>

    <div class="profile-summary">
      <BookOpenCheck :size="19" aria-hidden="true" />
      <div>
        <strong>{{ preparation.targetRole }}</strong>
        <p>{{ preparation.profile.candidateSummary }}</p>
      </div>
    </div>

    <ol class="question-list">
      <li
        v-for="(question, index) in generation.questions"
        :key="index"
        class="question-item"
      >
        <div class="question-toolbar">
          <span>Q{{ String(index + 1).padStart(2, "0") }}</span>
          <button
            type="button"
            class="icon-action"
            :disabled="busy || !canRemove"
            :aria-label="`删除第 ${index + 1} 题`"
            title="删除题目"
            @click="emit('removeQuestion', index)"
          >
            <Trash2 :size="17" aria-hidden="true" />
          </button>
        </div>

        <label :for="`draft-question-${index}`">题目</label>
        <textarea
          :id="`draft-question-${index}`"
          :value="question.question"
          :disabled="busy"
          rows="3"
          maxlength="1000"
          @input="emit('updateQuestion', index, ($event.target as HTMLTextAreaElement).value)"
        ></textarea>

        <label :for="`draft-objective-${index}`">考察目标</label>
        <input
          :id="`draft-objective-${index}`"
          :value="question.objective"
          :disabled="busy"
          type="text"
          maxlength="500"
          @input="emit('updateObjective', index, ($event.target as HTMLInputElement).value)"
        />

        <dl class="question-meta">
          <div>
            <dt>出题依据</dt>
            <dd>{{ question.basisSources.map((source) => sourceLabels[source]).join(" / ") }}</dd>
          </div>
          <div>
            <dt>知识来源</dt>
            <dd>{{ knowledgeSourceLabel(question) }}</dd>
          </div>
        </dl>
      </li>
    </ol>

    <p v-if="!canRemove" class="minimum-note">正式面试至少保留 3 道题。</p>

    <footer class="draft-actions">
      <button type="button" class="secondary-action" :disabled="busy" @click="emit('cancel')">
        <X :size="17" aria-hidden="true" />
        返回修改材料
      </button>
      <button type="button" class="secondary-action" :disabled="busy" @click="emit('regenerate')">
        <RefreshCw :size="17" aria-hidden="true" />
        {{ busy ? "正在重新生成…" : "重新生成" }}
      </button>
      <button
        type="button"
        class="primary-action"
        :disabled="busy || !canConfirm"
        @click="emit('confirm')"
      >
        <Check :size="18" aria-hidden="true" />
        确认并开始面试
      </button>
    </footer>
  </section>
</template>

<style scoped lang="scss">
.draft {
  display: grid;
  gap: var(--space-lg);
}

.draft-heading,
.draft-actions,
.question-toolbar,
.profile-summary {
  display: flex;
  align-items: center;
}

.draft-heading {
  justify-content: space-between;
  gap: var(--space-lg);
  padding-block: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.draft-heading h2,
.draft-heading p,
.profile-summary p,
.minimum-note {
  margin: 0;
}

.draft-heading h2 {
  margin-top: var(--space-xs);
  color: var(--color-ink);
  font-size: var(--text-2xl);
  letter-spacing: 0;
}

.draft-kicker,
.question-count,
.question-toolbar > span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
  text-transform: uppercase;
}

.question-count {
  padding: var(--space-xs) var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  color: var(--color-ink-2);
  background: var(--color-paper-2);
}

.profile-summary {
  align-items: flex-start;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-left: var(--space-2xs) solid var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.profile-summary div {
  display: grid;
  gap: var(--space-xs);
}

.profile-summary strong {
  color: var(--color-ink);
}

.profile-summary p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.question-list {
  display: grid;
  gap: var(--space-md);
  margin: 0;
  padding: 0;
  list-style: none;
}

.question-item {
  display: grid;
  gap: var(--space-sm);
  padding: var(--space-lg);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.question-toolbar {
  justify-content: space-between;
}

.question-item > label,
.question-meta dt {
  color: var(--color-muted);
  font-size: var(--text-xs);
  font-weight: 700;
}

.question-item textarea,
.question-item input {
  width: 100%;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-paper);
  font: inherit;
  line-height: 1.55;
}

.question-item textarea {
  min-height: 5.5rem;
  resize: vertical;
}

.question-item input {
  min-height: 3rem;
}

.icon-action {
  display: inline-grid;
  width: 2.25rem;
  height: 2.25rem;
  padding: 0;
  place-items: center;
  border: var(--rule-thin) solid transparent;
  color: var(--color-muted);
  background: transparent;
}

.icon-action:not(:disabled):hover {
  border-color: var(--color-error-rule);
  color: var(--color-error);
  background: var(--color-error-soft);
}

.question-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
  margin: 0;
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.question-meta div {
  display: grid;
  align-content: start;
  gap: var(--space-xs);
}

.question-meta dd {
  margin: 0;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.minimum-note {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.draft-actions {
  justify-content: flex-end;
  gap: var(--space-sm);
  padding-top: var(--space-sm);
}

.draft-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 3rem;
  padding-inline: var(--space-lg);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  font-weight: 700;
}

.secondary-action {
  color: var(--color-ink-2);
  background: var(--color-paper);
}

.primary-action {
  border-color: var(--color-accent) !important;
  color: var(--color-accent-ink);
  background: var(--color-accent);
}

@media (max-width: 48rem) {
  .question-meta {
    grid-template-columns: minmax(0, 1fr);
  }

  .draft-actions {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
