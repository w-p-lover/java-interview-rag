<script setup lang="ts">
import { computed, onMounted } from "vue";

import type {
  InterviewReviewTaskQuery,
  InterviewSessionStartPayload,
  ProjectDeepDiveSessionStartPayload,
} from "@/api/interview";
import InterviewFeedbackPanel from "@/components/interview/InterviewFeedbackPanel.vue";
import InterviewConversationTimeline from "@/components/interview/InterviewConversationTimeline.vue";
import InterviewQuestionPanel from "@/components/interview/InterviewQuestionPanel.vue";
import InterviewRecentSessionsPanel from "@/components/interview/InterviewRecentSessionsPanel.vue";
import InterviewReportPanel from "@/components/interview/InterviewReportPanel.vue";
import InterviewReviewAssistantPanel from "@/components/interview/InterviewReviewAssistantPanel.vue";
import InterviewReviewPathPanel from "@/components/interview/InterviewReviewPathPanel.vue";
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
  recentSessions,
  recentSessionsLoading,
  recentSessionsError,
  restoringSessionId,
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
  conversationStage,
  conversationEvents,
  initialize,
  restore,
  start,
  startProjectDeepDive,
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

const recentSessionIds = computed(() => recentSessions.value.map((item) => item.id));

onMounted(initialize);

function handleStart(payload: InterviewSessionStartPayload): void {
  void start(payload);
}

function handleProjectDeepDiveStart(payload: ProjectDeepDiveSessionStartPayload): void {
  void startProjectDeepDive(payload);
}

function handleSubmit(answer: string): void {
  void submit(answer);
}

function handleRestore(sessionId: string): void {
  void restore(sessionId);
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

function handleAssistantScheduled(): void {
  void filterReviewTasks(reviewTaskQuery.value);
}
</script>

<template>
  <WorkspaceHeader mode="interview" />

  <main id="main-content" class="interview-workspace" tabindex="-1">
    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>

    <section v-if="restoring" class="loading-card" aria-live="polite">
      正在恢复上次面试…
    </section>

    <template v-else-if="!session">
      <InterviewSetupForm
        id="interview-setup"
        :busy="loading"
        @start="handleStart"
        @start-project-deep-dive="handleProjectDeepDiveStart"
      />
      <InterviewRecentSessionsPanel
        :sessions="recentSessions"
        :loading="recentSessionsLoading"
        :error="recentSessionsError"
        :restoring-session-id="restoringSessionId"
        @restore="handleRestore"
      />
      <InterviewReviewPathPanel :session-ids="recentSessionIds" />
    </template>

    <template v-else>
      <InterviewSessionBar
        :target-role="session.targetRole"
        :mode="session.mode"
        :status="session.status"
        :current-question-index="session.currentQuestionIndex"
        :question-count="session.questions.length"
        :progress="progress"
        :loading="loading"
        @reset="reset"
      />

      <InterviewConversationTimeline
        v-if="conversationEvents.length"
        :events="conversationEvents"
      />

      <div v-if="session.status === 'COMPLETED' && session.report" class="completion-stack">
        <InterviewReportPanel
          id="interview-report"
          :report="session.report"
          :target-role="session.targetRole"
          :mode="session.mode"
          @restart="reset"
        />
        <InterviewReviewAssistantPanel
          :session-id="session.id"
          :tasks="reviewTasks"
          :history="reviewHistory"
          @scheduled="handleAssistantScheduled"
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
        <InterviewReviewPathPanel :session-ids="recentSessionIds" />
      </div>

      <section v-else-if="currentQuestion" class="interview-grid">
        <p v-if="loading && conversationStage" class="conversation-stage" aria-live="polite">
          {{ conversationStage.summary }}
        </p>
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
  width: auto;
  margin-left: var(--workspace-rail);
  padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
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
  grid-template-columns: minmax(0, 1.55fr) minmax(20rem, 0.68fr);
  align-items: stretch;
  gap: var(--space-lg);
  overflow: visible;
  background: transparent;
}

.completion-stack {
  display: grid;
  gap: var(--space-md);
}

.conversation-stage {
  grid-column: 1 / -1;
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  border-left: 3px solid var(--color-accent);
  color: var(--color-muted);
  background: var(--color-paper-2);
  font-size: var(--text-sm);
}

@media (max-width: 62rem) {
  .interview-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 68rem) {
  .interview-workspace {
    margin-left: 0;
  }
}

@media (max-width: 40rem) {
  .interview-workspace {
    width: calc(100% - (var(--space-md) * 2));
    margin-inline: auto;
    padding-inline: 0;
    padding-block: var(--space-md) var(--space-xl);
  }
}
</style>
