import { shallowRef } from "vue";

import {
  askInterviewReviewAssistant,
  type InterviewReviewAssistantRequest,
  type InterviewReviewScheduleConfirmation,
  type InterviewReviewTask,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

export interface PendingReviewScheduleConfirmation
  extends InterviewReviewScheduleConfirmation {
  questionIndex: number;
  question: string;
}

export function useInterviewReviewAssistant() {
  const busy = shallowRef(false);
  const answer = shallowRef("");
  const error = shallowRef("");
  const pendingConfirmation = shallowRef<PendingReviewScheduleConfirmation | null>(null);

  async function queryTasks(sessionId: string, message: string): Promise<boolean> {
    const normalizedMessage = message.trim();
    if (!normalizedMessage) {
      error.value = "请输入要查询的内容";
      return false;
    }
    pendingConfirmation.value = null;
    return send({
      message: `请查询会话 ${sessionId} 的复习任务。用户问题：${normalizedMessage}`,
    }, "查询复习任务失败");
  }

  async function requestSchedule(
    sessionId: string,
    task: InterviewReviewTask,
    plannedReviewDate: string,
  ): Promise<boolean> {
    const confirmation: PendingReviewScheduleConfirmation = {
      sessionId,
      taskId: task.id,
      plannedReviewDate,
      questionIndex: task.questionIndex,
      question: task.question,
    };
    const succeeded = await send({
      message: `请准备把会话 ${sessionId} 的复习任务 ${task.id} 调整到 ${plannedReviewDate}。先说明这次改期并请求确认，不要执行写入。`,
    }, "提出改期失败");
    pendingConfirmation.value = succeeded ? confirmation : null;
    return succeeded;
  }

  async function confirmSchedule(): Promise<boolean> {
    const confirmation = pendingConfirmation.value;
    if (!confirmation) {
      error.value = "没有等待确认的改期操作";
      return false;
    }
    const succeeded = await send({
      message: `我确认把会话 ${confirmation.sessionId} 的复习任务 ${confirmation.taskId} 调整到 ${confirmation.plannedReviewDate}。`,
      scheduleConfirmation: {
        sessionId: confirmation.sessionId,
        taskId: confirmation.taskId,
        plannedReviewDate: confirmation.plannedReviewDate,
      },
    }, "确认改期失败");
    if (succeeded) {
      pendingConfirmation.value = null;
    }
    return succeeded;
  }

  function cancelConfirmation(): void {
    pendingConfirmation.value = null;
    answer.value = "";
    error.value = "";
  }

  async function send(
    payload: InterviewReviewAssistantRequest,
    action: string,
  ): Promise<boolean> {
    if (busy.value) {
      return false;
    }
    busy.value = true;
    error.value = "";
    answer.value = "";
    try {
      const response = await askInterviewReviewAssistant(payload);
      answer.value = response.answer;
      return true;
    } catch (requestError) {
      error.value = resolveApiError(requestError, action);
      return false;
    } finally {
      busy.value = false;
    }
  }

  return {
    busy,
    answer,
    error,
    pendingConfirmation,
    queryTasks,
    requestSchedule,
    confirmSchedule,
    cancelConfirmation,
  };
}
