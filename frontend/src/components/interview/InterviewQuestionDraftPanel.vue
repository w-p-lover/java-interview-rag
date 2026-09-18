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
  InterviewDraftStatus,
  InterviewFocusSource,
  InterviewDifficulty,
  InterviewPreparationResult,
  InterviewQuestion,
  InterviewQuestionGenerationResult,
  InterviewQuestionSource,
  InterviewSkill,
} from "@/api/interview";

const props = defineProps<{
  preparation: DeepReadonly<InterviewPreparationResult>;
  generation: DeepReadonly<InterviewQuestionGenerationResult>;
  draftStatus: InterviewDraftStatus;
  targetQuestionCount: number;
  actualQuestionCount: number;
  unavailableSources: readonly InterviewQuestionSource[];
  failureMessage: string | null;
  reused: boolean;
  replacingQuestionIndex: number | null;
  busy: boolean;
  canConfirm: boolean;
}>();

const emit = defineEmits<{
  updateQuestion: [index: number, value: string];
  updateObjective: [index: number, value: string];
  removeQuestion: [index: number];
  replaceQuestion: [index: number];
  regenerate: [];
  cancel: [];
  confirm: [];
}>();

const canRemove = computed(() => props.generation.questions.length > 3);
const unavailableSourceLabel = computed(() => props.unavailableSources
  .map((source) => questionSourceLabels[source])
  .join(" / "));

const sourceLabels: Record<InterviewFocusSource, string> = {
  TARGET_ROLE: "目标岗位",
  RESUME: "简历",
  JOB_DESCRIPTION: "岗位描述",
};

const questionSourceLabels: Record<InterviewQuestionSource, string> = {
  SKILL_FIXED: "Skill 固定题",
  TARGET_ROLE: "目标岗位",
  RESUME: "简历事实",
  JOB_DESCRIPTION: "岗位描述",
  KNOWLEDGE: "知识资料",
  PROJECT: "项目事实",
  LEGACY: "历史题单",
};

const skillLabels: Record<InterviewSkill, string> = {
  JAVA_CORE: "Java 核心",
  SPRING: "Spring",
  DATABASE: "数据库",
  SYSTEM_DESIGN: "系统设计",
  RAG_AI: "RAG / AI",
  GENERAL: "综合",
};

const difficultyLabels: Record<InterviewDifficulty, string> = {
  BASIC: "基础",
  INTERMEDIATE: "进阶",
  ADVANCED: "高级场景",
};

function questionSourceLabel(index: number): string {
  const allocation = props.generation.configuration.questionAllocations[index];
  if (!allocation) {
    return "未记录";
  }
  const supporting = allocation.supportingSources
    .filter((source) => source !== allocation.primarySource)
    .map((source) => questionSourceLabels[source]);
  return [questionSourceLabels[allocation.primarySource], ...supporting].join(" / ");
}

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
      <div class="draft-heading-meta">
        <span v-if="reused" class="reused-label">已复用</span>
        <span class="question-count">
          {{ actualQuestionCount }} / {{ targetQuestionCount }} 道
        </span>
      </div>
    </header>

    <div class="profile-summary">
      <BookOpenCheck :size="19" aria-hidden="true" />
      <div>
        <strong>{{ preparation.targetRole }}</strong>
        <p>{{ preparation.profile.candidateSummary }}</p>
        <div class="configuration-summary">
          <span>{{ skillLabels[generation.configuration.skill] }}</span>
          <span>{{ difficultyLabels[generation.configuration.difficulty] }}</span>
          <span>{{ generation.configuration.strategyVersion }}</span>
        </div>
      </div>
    </div>

    <div v-if="draftStatus === 'PARTIAL'" class="partial-status" role="status">
      <strong>当前草稿未达到完整来源结果</strong>
      <span>{{ failureMessage || "已保留当前可用题目，可以替换或重新生成。" }}</span>
      <span v-if="unavailableSourceLabel">不可用来源：{{ unavailableSourceLabel }}</span>
    </div>

    <ol class="question-list">
      <li
        v-for="(question, index) in generation.questions"
        :key="index"
        class="question-item"
      >
        <div class="question-toolbar">
          <span>Q{{ String(index + 1).padStart(2, "0") }}</span>
          <div class="question-toolbar-actions">
            <button
              type="button"
              class="icon-action replace-action"
              :disabled="busy"
              :aria-label="`替换第 ${index + 1} 题`"
              :title="replacingQuestionIndex === index ? '正在替换' : '替换题目'"
              @click="emit('replaceQuestion', index)"
            >
              <RefreshCw :size="17" aria-hidden="true" />
            </button>
            <button
              type="button"
              class="icon-action delete-action"
              :disabled="busy || !canRemove"
              :aria-label="`删除第 ${index + 1} 题`"
              title="删除题目"
              @click="emit('removeQuestion', index)"
            >
              <Trash2 :size="17" aria-hidden="true" />
            </button>
          </div>
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
            <dt>题目来源</dt>
            <dd>{{ questionSourceLabel(index) }}</dd>
          </div>
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

.draft-heading-meta,
.question-toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
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
.reused-label,
.question-toolbar > span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
  text-transform: uppercase;
}

.reused-label {
  color: var(--color-success-rule);
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

.configuration-summary {
  display: flex !important;
  flex-wrap: wrap;
  gap: var(--space-xs) !important;
  margin-top: var(--space-xs);
}

.configuration-summary span {
  padding: var(--space-2xs) var(--space-xs);
  border: var(--rule-thin) solid var(--color-rule-2);
  color: var(--color-ink-2);
  background: var(--color-surface);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.profile-summary strong {
  color: var(--color-ink);
}

.profile-summary p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.partial-status {
  display: grid;
  gap: var(--space-xs);
  padding: var(--space-md);
  border-left: var(--space-2xs) solid var(--color-signal-rule);
  color: var(--color-muted);
  background: var(--color-signal-soft);
  font-size: var(--text-sm);
}

.partial-status strong {
  color: var(--color-ink);
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

.delete-action:not(:disabled):hover {
  border-color: var(--color-error-rule);
  color: var(--color-error);
  background: var(--color-error-soft);
}

.replace-action:not(:disabled):hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.question-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
