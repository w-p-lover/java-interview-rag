import { computed, shallowRef } from "vue";

import {
  generateInterviewReviewTasks,
  getInterviewReviewTasks,
  getInterviewSession,
  startInterviewSession,
  submitInterviewAnswer,
  type InterviewReviewTask,
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
      reviewTasks.value = await getInterviewReviewTasks(sessionId);
    } catch (requestError) {
      reviewTasksError.value = resolveApiError(requestError, "恢复复习任务失败");
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
    } catch (requestError) {
      reviewTasksError.value = resolveApiError(requestError, "生成复习任务失败");
    } finally {
      reviewTasksLoading.value = false;
    }
  }

  function clearReviewTasks(): void {
    reviewTasks.value = [];
    reviewTasksError.value = "";
    reviewTasksGenerated.value = false;
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
    restore,
    start,
    submit,
    generateReviewTasks,
    reset,
  };
}
