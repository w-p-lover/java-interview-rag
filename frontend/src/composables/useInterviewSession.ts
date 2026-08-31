import { computed, shallowRef } from "vue";
import axios from "axios";

import {
  completeInterviewReviewTask,
  generateInterviewReviewTasks,
  getInterviewReviewHistory,
  getInterviewReviewTasks,
  getInterviewSession,
  scheduleInterviewReviewTask,
  startInterviewSession,
  startProjectDeepDiveSession as requestProjectDeepDiveSession,
  submitInterviewAnswer,
  submitInterviewReviewAttempt,
  type InterviewReviewAttempt,
  type InterviewReviewSessionHistory,
  type InterviewReviewTask,
  type InterviewReviewTaskQuery,
  type InterviewSession,
  type InterviewSessionStartPayload,
  type ProjectDeepDiveSessionStartPayload,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

const SESSION_STORAGE_KEY = "java-interview-rag.active-interview-session";
const RECENT_SESSIONS_STORAGE_KEY = "java-interview-rag.recent-interview-sessions";
const RECENT_SESSION_LIMIT = 8;

export interface RecentInterviewSession {
  id: string;
  targetRole: string;
  mode: InterviewSession["mode"];
  status: InterviewSession["status"];
  currentQuestionIndex: number;
  questionCount: number;
  updatedAt: string;
}

export function useInterviewSession() {
  const session = shallowRef<InterviewSession | null>(null);
  const loading = shallowRef(false);
  const restoring = shallowRef(false);
  const error = shallowRef("");
  const recentSessions = shallowRef<RecentInterviewSession[]>([]);
  const recentSessionsLoading = shallowRef(false);
  const recentSessionsError = shallowRef("");
  const restoringSessionId = shallowRef("");
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

  async function initialize(): Promise<void> {
    await restore();
    if (!session.value) {
      await loadRecentSessions();
    }
  }

  async function restore(requestedSessionId?: string): Promise<void> {
    const sessionId = requestedSessionId || window.localStorage.getItem(SESSION_STORAGE_KEY);
    if (!sessionId) {
      return;
    }
    restoring.value = true;
    restoringSessionId.value = sessionId;
    error.value = "";
    try {
      const restored = await getInterviewSession(sessionId);
      session.value = restored;
      window.localStorage.setItem(SESSION_STORAGE_KEY, restored.id);
      rememberRecentSession(restored);
      if (restored.status === "COMPLETED") {
        await loadReviewTasks(restored.id);
      }
    } catch (requestError) {
      if (axios.isAxiosError(requestError) && requestError.response?.status === 404) {
        forgetRecentSession(sessionId);
        if (window.localStorage.getItem(SESSION_STORAGE_KEY) === sessionId) {
          window.localStorage.removeItem(SESSION_STORAGE_KEY);
        }
      }
      error.value = resolveApiError(requestError, "恢复面试会话失败");
    } finally {
      restoring.value = false;
      restoringSessionId.value = "";
    }
  }

  async function loadRecentSessions(): Promise<void> {
    const sessionIds = readRecentSessionIds();
    if (!sessionIds.length) {
      recentSessions.value = [];
      recentSessionsError.value = "";
      return;
    }
    recentSessionsLoading.value = true;
    recentSessionsError.value = "";
    const results = await Promise.all(sessionIds.map(async (sessionId) => {
      try {
        return { sessionId, restored: await getInterviewSession(sessionId), error: null };
      } catch (requestError) {
        return { sessionId, restored: null, error: requestError };
      }
    }));
    const missingSessionIds = results
      .filter((result) => axios.isAxiosError(result.error) && result.error.response?.status === 404)
      .map((result) => result.sessionId);
    if (missingSessionIds.length) {
      writeRecentSessionIds(sessionIds.filter((sessionId) => !missingSessionIds.includes(sessionId)));
    }
    recentSessions.value = results
      .filter((result): result is typeof result & { restored: InterviewSession } => result.restored !== null)
      .map((result) => toRecentSession(result.restored));
    if (results.some((result) => result.error && !missingSessionIds.includes(result.sessionId))) {
      recentSessionsError.value = "部分最近面试暂时无法读取，请稍后重试。";
    }
    recentSessionsLoading.value = false;
  }

  async function start(payload: InterviewSessionStartPayload): Promise<void> {
    loading.value = true;
    error.value = "";
    clearReviewTasks();
    try {
      const created = await startInterviewSession(payload);
      session.value = created;
      window.localStorage.setItem(SESSION_STORAGE_KEY, created.id);
      rememberRecentSession(created);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "创建面试会话失败");
    } finally {
      loading.value = false;
    }
  }

  async function startProjectDeepDive(payload: ProjectDeepDiveSessionStartPayload): Promise<void> {
    loading.value = true;
    error.value = "";
    clearReviewTasks();
    try {
      const created = await requestProjectDeepDiveSession(payload);
      session.value = created;
      window.localStorage.setItem(SESSION_STORAGE_KEY, created.id);
      rememberRecentSession(created);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "创建项目深挖会话失败");
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
      rememberRecentSession(updated);
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
    void loadRecentSessions();
  }

  function rememberRecentSession(interviewSession: InterviewSession): void {
    recentSessions.value = [
      toRecentSession(interviewSession),
      ...recentSessions.value.filter((item) => item.id !== interviewSession.id),
    ].slice(0, RECENT_SESSION_LIMIT);
    writeRecentSessionIds([
      interviewSession.id,
      ...readRecentSessionIds().filter((sessionId) => sessionId !== interviewSession.id),
    ]);
  }

  function forgetRecentSession(sessionId: string): void {
    recentSessions.value = recentSessions.value.filter((item) => item.id !== sessionId);
    writeRecentSessionIds(readRecentSessionIds().filter((item) => item !== sessionId));
  }

  function readRecentSessionIds(): string[] {
    const value = window.localStorage.getItem(RECENT_SESSIONS_STORAGE_KEY);
    if (!value) {
      return [];
    }
    try {
      const parsed: unknown = JSON.parse(value);
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed
        .filter((item): item is string => typeof item === "string")
        .map((item) => item.trim())
        .filter((item, index, values) => item.length > 0
          && item.length <= 100 && values.indexOf(item) === index)
        .slice(0, RECENT_SESSION_LIMIT);
    } catch {
      return [];
    }
  }

  function writeRecentSessionIds(sessionIds: string[]): void {
    window.localStorage.setItem(
      RECENT_SESSIONS_STORAGE_KEY,
      JSON.stringify(sessionIds.slice(0, RECENT_SESSION_LIMIT)),
    );
  }

  function toRecentSession(interviewSession: InterviewSession): RecentInterviewSession {
    return {
      id: interviewSession.id,
      targetRole: interviewSession.targetRole,
      mode: interviewSession.mode,
      status: interviewSession.status,
      currentQuestionIndex: interviewSession.currentQuestionIndex,
      questionCount: interviewSession.questions.length,
      updatedAt: interviewSession.updatedAt,
    };
  }

  return {
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
    initialize,
    restore,
    start,
    startProjectDeepDive,
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
