import { computed, shallowRef } from "vue";

import {
  getInterviewSession,
  startInterviewSession,
  submitInterviewAnswer,
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
      session.value = await getInterviewSession(sessionId);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "恢复面试会话失败");
    } finally {
      restoring.value = false;
    }
  }

  async function start(payload: InterviewSessionStartPayload): Promise<void> {
    loading.value = true;
    error.value = "";
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
      session.value = await submitInterviewAnswer(
        activeSession.id,
        activeSession.currentQuestionIndex,
        activeSession.version,
        answer,
      );
    } catch (requestError) {
      error.value = resolveApiError(requestError, "提交回答失败");
    } finally {
      loading.value = false;
    }
  }

  function reset(): void {
    if (loading.value) {
      return;
    }
    session.value = null;
    error.value = "";
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
  }

  return {
    session,
    loading,
    restoring,
    error,
    currentQuestion,
    latestAnswer,
    restore,
    start,
    submit,
    reset,
  };
}
