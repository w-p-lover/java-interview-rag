import { computed, shallowRef } from "vue";

import {
  completeInterviewReviewTask,
  generateInterviewReviewTasks,
  getInterviewReviewHistory,
  getInterviewReviewTasks,
  getInterviewSession,
  scheduleInterviewReviewTask,
  startInterviewSession,
  submitInterviewAnswer,
  submitInterviewReviewAttempt,
  type InterviewReviewAttempt,
  type InterviewReviewSessionHistory,
  type InterviewReviewTask,
  type InterviewReviewTaskQuery,
  type InterviewSession,
  type InterviewSessionStartPayload,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

const SESSION_STORAGE_KEY = "java-interview-rag.active-interview-session";

export function useInterviewSession() {
  const session = shallowRef<InterviewSession | null>(null);
  const loading = shallowRef(false);
  const restoring = shallowRef(false);
  const error = shallowRef("");
  const reviewTasks = shallowRef<InterviewReviewTask[]>([]);
  const reviewTasksLoading = shallowRef(false);
  const reviewTasksError = shallowRef("");
  const reviewTasksGenerated = shallowRef(false);
  const completingReviewTaskId = shallowRef("");
  const reviewTaskActionError = shallowRef("");
  const reviewTaskQuery = shallowRef<InterviewReviewTaskQuery>({});
  const schedulingReviewTaskId = shallowRef("");
  const reviewAttempts = shallowRef<Record<string, InterviewReviewAttempt[]>>({});
  const reviewHistory = shallowRef<InterviewReviewSessionHistory | null>(null);
  const reviewAttemptsLoadingTaskId = shallowRef("");
  const submittingReviewAttemptTaskId = shallowRef("");
  const reviewAttemptError = shallowRef("");

  const currentQuestion = computed(() => {
    if (!session.value || session.value.status === "COMPLETED") {
      return null;
    }
    return session.value.questions[session.value.currentQuestionIndex] ?? null;
  });

  const latestAnswer = computed(() => session.value?.answers.at(-1) ?? null);

  async function restore(): Promise<void> {
    const sessionId = window.localStorage.getItem(SESSION_STORAGE_KEY);
    if (!sessionId) {
      return;
    }
    restoring.value = true;
    error.value = "";
    try {
      const restored = await getInterviewSession(sessionId);
      session.value = restored;
      if (restored.status === "COMPLETED") {
        await loadReviewTasks(restored.id);
      }
    } catch (requestError) {
      error.value = resolveApiError(requestError, "恢复面试会话失败");
    } finally {
      restoring.value = false;
    }
  }

  async function start(payload: InterviewSessionStartPayload): Promise<void> {
    loading.value = true;
    error.value = "";
    clearReviewTasks();
    try {
      const created = await startInterviewSession(payload);
      session.value = created;
      window.localStorage.setItem(SESSION_STORAGE_KEY, created.id);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "创建面试会话失败");
    } finally {
      loading.value = false;
    }
  }

  async function submit(answer: string): Promise<void> {
    const activeSession = session.value;
    if (!activeSession || activeSession.status === "COMPLETED") {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      const updated = await submitInterviewAnswer(
        activeSession.id,
        activeSession.currentQuestionIndex,
        activeSession.version,
        answer,
      );
      session.value = updated;
      if (updated.status === "COMPLETED") {
        await generateReviewTasks();
      }
    } catch (requestError) {
      error.value = resolveApiError(requestError, "提交回答失败");
    } finally {
      loading.value = false;
    }
  }

  async function loadReviewTasks(sessionId: string): Promise<void> {
    reviewTasksLoading.value = true;
    reviewTasksError.value = "";
    reviewTasksGenerated.value = false;
    try {
      await loadReviewTaskList(sessionId);
      await loadReviewHistory(sessionId);
    } catch (requestError) {
      reviewTasksError.value = resolveApiError(requestError, "恢复复习任务失败");
    } finally {
      reviewTasksLoading.value = false;
    }
  }

  async function loadReviewTaskList(
    sessionId: string,
    query: InterviewReviewTaskQuery = reviewTaskQuery.value,
  ): Promise<void> {
    reviewTasks.value = await getInterviewReviewTasks(sessionId, query);
  }

  async function filterReviewTasks(query: InterviewReviewTaskQuery): Promise<void> {
    const completedSession = session.value;
    if (!completedSession || completedSession.status !== "COMPLETED" || reviewTasksLoading.value) {
      return;
    }
    reviewTasksLoading.value = true;
    reviewTasksError.value = "";
    try {
      await loadReviewTaskList(completedSession.id, query);
      reviewTaskQuery.value = query;
    } catch (requestError) {
      reviewTasksError.value = resolveApiError(requestError, "筛选复习任务失败");
    } finally {
      reviewTasksLoading.value = false;
    }
  }

  async function generateReviewTasks(): Promise<void> {
    const completedSession = session.value;
    if (!completedSession || completedSession.status !== "COMPLETED") {
      return;
    }
    reviewTasksLoading.value = true;
    reviewTasksError.value = "";
    try {
      reviewTasks.value = await generateInterviewReviewTasks(completedSession.id);
      reviewTasksGenerated.value = true;
      await loadReviewHistory(completedSession.id);
    } catch (requestError) {
      reviewTasksError.value = resolveApiError(requestError, "生成复习任务失败");
    } finally {
      reviewTasksLoading.value = false;
    }
  }

  async function loadReviewHistory(sessionId: string): Promise<void> {
    reviewAttemptsLoadingTaskId.value = "*";
    reviewAttemptError.value = "";
    try {
      const history = await getInterviewReviewHistory(sessionId);
      reviewHistory.value = history;
      reviewAttempts.value = Object.fromEntries(
        history.tasks.map((task) => [task.reviewTaskId, task.attempts]),
      );
    } catch (requestError) {
      reviewAttemptError.value = resolveApiError(requestError, "恢复复习回答失败");
    } finally {
      reviewAttemptsLoadingTaskId.value = "";
    }
  }

  async function submitReviewAttempt(taskId: string, answer: string): Promise<void> {
    const completedSession = session.value;
    if (!completedSession || completedSession.status !== "COMPLETED"
      || submittingReviewAttemptTaskId.value) {
      return;
    }
    const existingAttempts = reviewAttempts.value[taskId] ?? [];
    submittingReviewAttemptTaskId.value = taskId;
    reviewAttemptError.value = "";
    try {
      await submitInterviewReviewAttempt(
        completedSession.id,
        taskId,
        answer,
        existingAttempts.length + 1,
      );
      await loadReviewHistory(completedSession.id);
    } catch (requestError) {
      reviewAttemptError.value = resolveApiError(requestError, "提交复习回答失败");
    } finally {
      submittingReviewAttemptTaskId.value = "";
    }
  }

  async function completeReviewTask(taskId: string): Promise<void> {
    const completedSession = session.value;
    if (!completedSession || completedSession.status !== "COMPLETED" || completingReviewTaskId.value) {
      return;
    }
    completingReviewTaskId.value = taskId;
    reviewTaskActionError.value = "";
    try {
      await completeInterviewReviewTask(completedSession.id, taskId);
      await loadReviewTaskList(completedSession.id);
    } catch (requestError) {
      reviewTaskActionError.value = resolveApiError(requestError, "完成复习任务失败");
    } finally {
      completingReviewTaskId.value = "";
    }
  }

  async function scheduleReviewTask(taskId: string, plannedReviewDate: string | null): Promise<void> {
    const completedSession = session.value;
    if (!completedSession || completedSession.status !== "COMPLETED" || schedulingReviewTaskId.value) {
      return;
    }
    schedulingReviewTaskId.value = taskId;
    reviewTaskActionError.value = "";
    try {
      await scheduleInterviewReviewTask(completedSession.id, taskId, plannedReviewDate);
      await loadReviewTaskList(completedSession.id);
    } catch (requestError) {
      reviewTaskActionError.value = resolveApiError(requestError, "更新复习计划失败");
    } finally {
      schedulingReviewTaskId.value = "";
    }
  }

  function clearReviewTasks(): void {
    reviewTasks.value = [];
    reviewTasksError.value = "";
    reviewTasksGenerated.value = false;
    reviewTaskQuery.value = {};
    completingReviewTaskId.value = "";
    reviewTaskActionError.value = "";
    schedulingReviewTaskId.value = "";
    reviewAttempts.value = {};
    reviewHistory.value = null;
    reviewAttemptsLoadingTaskId.value = "";
    submittingReviewAttemptTaskId.value = "";
    reviewAttemptError.value = "";
  }

  function reset(): void {
    if (loading.value) {
      return;
    }
    session.value = null;
    error.value = "";
    clearReviewTasks();
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
  }

  return {
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
    restore,
    start,
    submit,
    generateReviewTasks,
    completeReviewTask,
    filterReviewTasks,
    scheduleReviewTask,
    reviewAttempts,
    reviewHistory,
    reviewAttemptsLoadingTaskId,
    submittingReviewAttemptTaskId,
    reviewAttemptError,
    submitReviewAttempt,
    reset,
  };
}
