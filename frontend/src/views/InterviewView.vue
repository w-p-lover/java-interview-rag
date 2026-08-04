<script setup lang="ts">
import { computed, onMounted } from "vue";

import type { InterviewReviewTaskQuery, InterviewSessionStartPayload } from "@/api/interview";
import InterviewFeedbackPanel from "@/components/interview/InterviewFeedbackPanel.vue";
import InterviewQuestionPanel from "@/components/interview/InterviewQuestionPanel.vue";
import InterviewReportPanel from "@/components/interview/InterviewReportPanel.vue";
import InterviewReviewTaskPanel from "@/components/interview/InterviewReviewTaskPanel.vue";
import InterviewSessionBar from "@/components/interview/InterviewSessionBar.vue";
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
  reviewTaskQuery,
  schedulingReviewTaskId,
  reviewAttempts,
  reviewHistory,
  reviewAttemptsLoadingTaskId,
  submittingReviewAttemptTaskId,
  reviewAttemptError,
  restore,
  start,
  submit,
  generateReviewTasks,
  completeReviewTask,
  filterReviewTasks,
  scheduleReviewTask,
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

function handleReviewFilter(query: InterviewReviewTaskQuery): void {
  void filterReviewTasks(query);
}

function handleReviewSchedule(taskId: string, plannedReviewDate: string | null): void {
  void scheduleReviewTask(taskId, plannedReviewDate);
}

function handleReviewAttempt(taskId: string, answer: string): void {
  void submitReviewAttempt(taskId, answer);
}
</script>

<template>
  <WorkspaceHeader mode="interview" />

  <main id="main-content" class="interview-workspace" tabindex="-1">
    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>

    <section v-if="restoring" class="loading-card" aria-live="polite">
      正在恢复上次面试…
    </section>

    <InterviewSetupForm
      v-else-if="!session"
      id="interview-setup"
      :busy="loading"
      @start="handleStart"
    />

    <template v-else>
      <InterviewSessionBar
        :target-role="session.targetRole"
        :status="session.status"
        :current-question-index="session.currentQuestionIndex"
        :question-count="session.questions.length"
        :progress="progress"
        :loading="loading"
        @reset="reset"
      />

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
          :scheduling-task-id="schedulingReviewTaskId"
          :action-error="reviewTaskActionError"
          :query="reviewTaskQuery"
          :attempts-by-task-id="reviewAttempts"
          :attempts-loading-task-id="reviewAttemptsLoadingTaskId"
          :submitting-attempt-task-id="submittingReviewAttemptTaskId"
          :attempt-error="reviewAttemptError"
          :history="reviewHistory"
          @retry="handleReviewRetry"
          @complete="handleReviewComplete"
          @filter="handleReviewFilter"
          @schedule="handleReviewSchedule"
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
  </main>
</template>

<style scoped lang="scss">
.interview-workspace {
  display: grid;
  gap: var(--space-md);
  width: min(calc(100% - (var(--space-lg) * 2)), 82rem);
  margin-inline: auto;
  padding-block: var(--space-lg) var(--space-2xl);
}

.error-banner,
.loading-card {
  margin: 0;
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

.interview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(19rem, 0.72fr);
  align-items: start;
  gap: var(--space-md);
}

.completion-stack {
  display: grid;
  gap: var(--space-md);
}

@media (max-width: 62rem) {
  .interview-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 40rem) {
  .interview-workspace {
    width: calc(100% - (var(--space-md) * 2));
    padding-block: var(--space-md) var(--space-xl);
  }
}
</style>
