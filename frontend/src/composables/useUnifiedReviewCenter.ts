import { computed, readonly, shallowRef } from "vue";

import {
  getInterviewReviewPath,
  type InterviewReviewPathResult,
} from "@/api/interview";
import {
  getKnowledgeReviewPath,
  type KnowledgeReviewPathResult,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";
import { readRecentInterviewSessionIds } from "@/utils/interviewSessionStorage";

export type UnifiedReviewKind = "INTERVIEW" | "KNOWLEDGE";
export type UnifiedReviewTiming = "OVERDUE" | "TODAY" | "UPCOMING";

export interface UnifiedReviewItem {
  key: string;
  kind: UnifiedReviewKind;
  timing: UnifiedReviewTiming;
  taskId: string;
  sessionId: string | null;
  title: string;
  context: string;
  status: "PENDING" | "COMPLETED";
  plannedReviewDate: string | null;
  recommendedReviewDate: string;
  intervalDays: number;
  attemptCount: number;
  latestOverallScore: number | null;
  bestOverallScore: number | null;
  reason: string;
}

function localDateKey(date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function timingOf(date: string, today: string): UnifiedReviewTiming {
  if (date < today) {
    return "OVERDUE";
  }
  return date === today ? "TODAY" : "UPCOMING";
}

const timingRank: Record<UnifiedReviewTiming, number> = {
  OVERDUE: 0,
  TODAY: 1,
  UPCOMING: 2,
};

export function useUnifiedReviewCenter() {
  const interviewResult = shallowRef<InterviewReviewPathResult | null>(null);
  const knowledgeResult = shallowRef<KnowledgeReviewPathResult | null>(null);
  const interviewError = shallowRef("");
  const knowledgeError = shallowRef("");
  const loading = shallowRef(false);
  const sessionCount = shallowRef(0);

  const items = computed<UnifiedReviewItem[]>(() => {
    const today = localDateKey();
    const interviewItems = (interviewResult.value?.recommendations ?? []).map((item) => ({
      key: `interview-${item.sessionId}-${item.taskId}`,
      kind: "INTERVIEW" as const,
      timing: timingOf(item.recommendedReviewDate, today),
      taskId: item.taskId,
      sessionId: item.sessionId,
      title: item.objective,
      context: item.question,
      status: item.status,
      plannedReviewDate: item.plannedReviewDate,
      recommendedReviewDate: item.recommendedReviewDate,
      intervalDays: item.intervalDays,
      attemptCount: item.attemptCount,
      latestOverallScore: item.latestOverallScore,
      bestOverallScore: item.bestOverallScore,
      reason: item.reason,
    }));
    const knowledgeItems = (knowledgeResult.value?.recommendations ?? []).map((item) => ({
      key: `knowledge-${item.taskId}`,
      kind: "KNOWLEDGE" as const,
      timing: timingOf(item.recommendedReviewDate, today),
      taskId: item.taskId,
      sessionId: null,
      title: item.claim,
      context: item.question,
      status: item.status,
      plannedReviewDate: item.plannedReviewDate,
      recommendedReviewDate: item.recommendedReviewDate,
      intervalDays: item.intervalDays,
      attemptCount: item.attemptCount,
      latestOverallScore: item.latestOverallScore,
      bestOverallScore: item.bestOverallScore,
      reason: item.reason,
    }));
    return [...interviewItems, ...knowledgeItems].sort((left, right) =>
      Number(left.status === "COMPLETED") - Number(right.status === "COMPLETED")
      || timingRank[left.timing] - timingRank[right.timing]
      || left.recommendedReviewDate.localeCompare(right.recommendedReviewDate)
      || left.kind.localeCompare(right.kind)
      || left.key.localeCompare(right.key));
  });

  const summary = computed(() => ({
    total: items.value.length,
    due: items.value.filter((item) => item.timing !== "UPCOMING"
      && item.status === "PENDING").length,
    overdue: items.value.filter((item) => item.timing === "OVERDUE"
      && item.status === "PENDING").length,
    practiced: items.value.filter((item) => item.attemptCount > 0).length,
    interview: items.value.filter((item) => item.kind === "INTERVIEW").length,
    knowledge: items.value.filter((item) => item.kind === "KNOWLEDGE").length,
  }));

  async function load(): Promise<void> {
    if (loading.value) {
      return;
    }
    loading.value = true;
    interviewError.value = "";
    knowledgeError.value = "";
    const sessionIds = readRecentInterviewSessionIds();
    sessionCount.value = sessionIds.length;
    const interviewRequest = sessionIds.length
      ? getInterviewReviewPath(sessionIds)
      : Promise.resolve<InterviewReviewPathResult | null>(null);
    const [interview, knowledge] = await Promise.allSettled([
      interviewRequest,
      getKnowledgeReviewPath(),
    ]);
    if (interview.status === "fulfilled") {
      interviewResult.value = interview.value;
    } else {
      interviewResult.value = null;
      interviewError.value = resolveApiError(interview.reason, "面试复习路径加载失败");
    }
    if (knowledge.status === "fulfilled") {
      knowledgeResult.value = knowledge.value;
    } else {
      knowledgeResult.value = null;
      knowledgeError.value = resolveApiError(knowledge.reason, "知识复习路径加载失败");
    }
    loading.value = false;
  }

  return {
    items,
    summary,
    interviewResult: readonly(interviewResult),
    knowledgeResult: readonly(knowledgeResult),
    interviewError: readonly(interviewError),
    knowledgeError: readonly(knowledgeError),
    loading: readonly(loading),
    sessionCount: readonly(sessionCount),
    load,
  };
}
