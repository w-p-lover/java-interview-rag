<script setup lang="ts">
import { computed, onMounted } from "vue";

import type { InterviewSessionStartPayload } from "@/api/interview";
import InterviewFeedbackPanel from "@/components/interview/InterviewFeedbackPanel.vue";
import InterviewQuestionPanel from "@/components/interview/InterviewQuestionPanel.vue";
import InterviewReportPanel from "@/components/interview/InterviewReportPanel.vue";
import InterviewReviewTaskPanel from "@/components/interview/InterviewReviewTaskPanel.vue";
import InterviewSetupForm from "@/components/interview/InterviewSetupForm.vue";
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import { useInterviewSession } from "@/composables/useInterviewSession";

const {
  session,
  loading,
  restoring,
  error,
  currentQuestion,
  latestAnswer,
  reviewTasks,
  reviewTasksLoading,
  reviewTasksError,
  reviewTasksGenerated,
  completingReviewTaskId,
  reviewTaskActionError,
  reviewAttempts,
  reviewAttemptsLoadingTaskId,
  submittingReviewAttemptTaskId,
  reviewAttemptError,
  restore,
  start,
  submit,
  generateReviewTasks,
  completeReviewTask,
  submitReviewAttempt,
  reset,
} = useInterviewSession();

const progress = computed(() => {
  if (!session.value) {
    return 0;
  }
  return Math.round((session.value.currentQuestionIndex / session.value.questions.length) * 100);
});

onMounted(restore);

function handleStart(payload: InterviewSessionStartPayload): void {
  void start(payload);
}

function handleSubmit(answer: string): void {
  void submit(answer);
}

function handleReviewRetry(): void {
  void generateReviewTasks();
}

function handleReviewComplete(taskId: string): void {
  void completeReviewTask(taskId);
}

function handleReviewAttempt(taskId: string, answer: string): void {
  void submitReviewAttempt(taskId, answer);
}
</script>

<template>
  <WorkspaceHeader mode="interview" />

  <main id="main-content" class="interview-workspace" tabindex="-1">
    <section class="interview-brief" aria-labelledby="interview-page-title">
      <div>
        <span>TEXT INTERVIEW / RAG GROUNDED</span>
        <h1 id="interview-page-title">从简历出题，沿回答继续深挖</h1>
        <p>
          这不是固定三题的问卷。你可以设置 3–8 道首轮题和最多 12 道整场上限；每次回答都会得到结构化评分，必要时追加有知识来源的追问。
        </p>
      </div>

      <dl>
        <div>
          <dt>出题依据</dt>
          <dd>目标岗位 + 简历 + 可选 JD</dd>
        </div>
        <div>
          <dt>回答反馈</dt>
          <dd>技术、完整性、表达</dd>
        </div>
        <div>
          <dt>会话状态</dt>
          <dd>{{ session ? (session.status === "COMPLETED" ? "已完成" : "进行中") : "等待开始" }}</dd>
        </div>
      </dl>
    </section>

    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>

    <section v-if="restoring" class="loading-card" aria-live="polite">
      正在恢复上一次面试会话…
    </section>

    <InterviewSetupForm
      v-else-if="!session"
      id="interview-setup"
      :busy="loading"
      @start="handleStart"
    />

    <template v-else>
      <section id="interview-progress" class="session-strip" aria-label="当前面试进度">
        <div>
          <span>{{ session.targetRole }}</span>
          <strong>
            {{ session.status === "COMPLETED" ? "面试完成" : `已回答 ${session.currentQuestionIndex} 道` }}
          </strong>
        </div>
        <div class="progress-track" aria-hidden="true">
          <span :style="{ width: `${progress}%` }"></span>
        </div>
        <button type="button" :disabled="loading" @click="reset">放弃并重新开始</button>
      </section>

      <div v-if="session.status === 'COMPLETED' && session.report" class="completion-stack">
        <InterviewReportPanel
          id="interview-report"
          :report="session.report"
          :target-role="session.targetRole"
          @restart="reset"
        />
        <InterviewReviewTaskPanel
          id="interview-review-tasks"
          :tasks="reviewTasks"
          :loading="reviewTasksLoading"
          :error="reviewTasksError"
          :generated="reviewTasksGenerated"
          :completing-task-id="completingReviewTaskId"
          :action-error="reviewTaskActionError"
          :attempts-by-task-id="reviewAttempts"
          :attempts-loading-task-id="reviewAttemptsLoadingTaskId"
          :submitting-attempt-task-id="submittingReviewAttemptTaskId"
          :attempt-error="reviewAttemptError"
          @retry="handleReviewRetry"
          @complete="handleReviewComplete"
          @submit-attempt="handleReviewAttempt"
        />
      </div>

      <section v-else-if="currentQuestion" class="interview-grid">
        <InterviewQuestionPanel
          id="current-question"
          :question="currentQuestion"
          :question-index="session.currentQuestionIndex"
          :question-count="session.questions.length"
          :max-question-count="session.maxQuestionCount"
          :busy="loading"
          @submit="handleSubmit"
        />
        <InterviewFeedbackPanel id="latest-feedback" :answer="latestAnswer" />
      </section>
    </template>

    <footer>
      <strong>Java Interview RAG</strong>
      <span>文本模拟面试 · 会话自动恢复 · 回答不可被并发覆盖</span>
    </footer>
  </main>
</template>

<style scoped lang="scss">
.interview-workspace {
  display: grid;
  gap: var(--space-xl);
  width: min(calc(100% - (var(--space-lg) * 2)), 84rem);
  margin-inline: auto;
  padding-block: var(--space-xl) var(--space-2xl);
}

.interview-brief {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(18rem, 0.65fr);
  align-items: end;
  gap: var(--space-2xl);
  padding-block: var(--space-lg) var(--space-xl);
}

.interview-brief > div {
  display: grid;
  gap: var(--space-md);
}

.interview-brief > div > span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

h1,
p,
dl {
  margin: 0;
}

h1 {
  max-width: 16ch;
  color: var(--color-ink);
  font-size: var(--text-display-s);
  letter-spacing: -0.045em;
  line-height: 1.08;
  text-wrap: balance;
}

.interview-brief p {
  max-width: 65ch;
  color: var(--color-muted);
  line-height: 1.7;
}

.interview-brief dl {
  display: grid;
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.interview-brief dl div {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
  gap: var(--space-md);
  padding-block: var(--space-sm);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.interview-brief dt,
.interview-brief dd {
  margin: 0;
  font-size: var(--text-sm);
}

.interview-brief dt {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.interview-brief dd {
  color: var(--color-ink-2);
  font-weight: 600;
}

.error-banner,
.loading-card {
  padding: var(--space-md);
  border-radius: var(--radius-control);
  font-size: var(--text-sm);
}

.error-banner {
  border: var(--rule-thin) solid var(--color-error-rule);
  color: var(--color-error);
  background: var(--color-error-soft);
}

.loading-card {
  border: var(--rule-thin) solid var(--color-rule);
  color: var(--color-muted);
  background: var(--color-surface);
}

.session-strip {
  display: grid;
  grid-template-columns: minmax(13rem, auto) minmax(10rem, 1fr) auto;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-md) var(--space-lg);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.session-strip > div:first-child {
  display: grid;
  gap: var(--space-2xs);
}

.session-strip span,
.session-strip button {
  font-size: var(--text-xs);
}

.session-strip > div:first-child span {
  color: var(--color-muted);
}

.session-strip strong {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.progress-track {
  height: var(--space-xs);
  overflow: hidden;
  border-radius: var(--radius-round);
  background: var(--color-rule);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-accent);
  transition: width var(--dur-long) var(--ease-out);
}

.session-strip button {
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-surface);
}

.interview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(18rem, 0.65fr);
  align-items: start;
  gap: var(--space-lg);
}

.completion-stack {
  display: grid;
  gap: var(--space-lg);
}

footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
  color: var(--color-muted);
  font-size: var(--text-xs);
}

footer strong {
  color: var(--color-ink);
  font-family: var(--font-display);
}

@media (max-width: 58rem) {
  .interview-brief,
  .interview-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .interview-brief {
    gap: var(--space-xl);
  }
}

@media (max-width: 40rem) {
  .interview-workspace {
    width: calc(100% - (var(--space-md) * 2));
    padding-block: var(--space-md) var(--space-xl);
  }

  .interview-brief {
    padding-block: var(--space-md);
  }

  h1 {
    font-size: clamp(2rem, 11vw, 2.75rem);
  }

  .session-strip {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-md);
    padding: var(--space-md);
  }

  footer {
    display: grid;
  }
}
</style>
