import { onScopeDispose, shallowRef } from "vue";

import {
  streamKnowledgeQuestion,
  type KnowledgeConversationTurn,
  type KnowledgeAnswerResponse,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

export const KNOWLEDGE_QUESTION_MAX_LENGTH = 2_000;
export const KNOWLEDGE_ANSWER_TIMEOUT_MS = 75_000;

export function useKnowledgeAnswer() {
  const question = shallowRef("");
  const submittedQuestion = shallowRef("");
  const result = shallowRef<KnowledgeAnswerResponse | null>(null);
  const loading = shallowRef(false);
  const streaming = shallowRef(false);
  const error = shallowRef("");
  const history = shallowRef<KnowledgeConversationTurn[]>([]);
  let activeController: AbortController | null = null;
  let timeoutHandle: ReturnType<typeof setTimeout> | null = null;

  async function submit(): Promise<void> {
    const normalizedQuestion = question.value.trim();
    if (!normalizedQuestion) {
      error.value = "请输入要查询的问题。";
      return;
    }
    if (normalizedQuestion.length > KNOWLEDGE_QUESTION_MAX_LENGTH) {
      error.value = `问题不能超过 ${KNOWLEDGE_QUESTION_MAX_LENGTH} 个字符。`;
      return;
    }

    loading.value = true;
    streaming.value = true;
    activeController = new AbortController();
    let timedOut = false;
    timeoutHandle = setTimeout(() => {
      timedOut = true;
      activeController?.abort();
    }, KNOWLEDGE_ANSWER_TIMEOUT_MS);
    error.value = "";
    result.value = {
      answer: "",
      grounded: false,
      sources: [],
      claimEvidence: [],
      retrievalStrategy: "HYBRID",
      degraded: false,
      degradationReason: null,
    };
    submittedQuestion.value = normalizedQuestion;
    try {
      await streamKnowledgeQuestion(normalizedQuestion, (event) => {
        if (event.type === "sources") {
          result.value = { ...result.value!, sources: event.sources,
            retrievalStrategy: event.retrievalStrategy, degraded: event.degraded,
            degradationReason: event.degradationReason };
          return;
        }
        if (event.type === "delta") {
          result.value = {
            answer: `${result.value?.answer ?? ""}${event.content}`,
            grounded: false,
            sources: result.value?.sources ?? [],
            claimEvidence: [],
            retrievalStrategy: result.value?.retrievalStrategy ?? "HYBRID",
            degraded: result.value?.degraded ?? false,
            degradationReason: result.value?.degradationReason ?? null,
          };
          return;
        }
        result.value = {
          answer: event.content,
          grounded: event.grounded,
          sources: event.sources,
          claimEvidence: event.claimEvidence,
          retrievalStrategy: event.retrievalStrategy,
          degraded: event.degraded,
          degradationReason: event.degradationReason,
        };
      }, history.value, activeController.signal);
      if (result.value?.answer && result.value.grounded) {
        history.value = [...history.value, { question: normalizedQuestion, answer: result.value.answer }].slice(-4);
      }
    } catch (requestError) {
      error.value = requestError instanceof DOMException && requestError.name === "AbortError"
        ? timedOut ? "回答等待超时，可先阅读已命中的资料。" : "本次问答已取消，可继续阅读已命中的资料。"
        : requestError instanceof Error
          ? requestError.message
          : resolveApiError(requestError, "知识问答失败");
    } finally {
      if (timeoutHandle) clearTimeout(timeoutHandle);
      timeoutHandle = null;
      activeController = null;
      loading.value = false;
      streaming.value = false;
    }
  }

  function clear(): void {
    activeController?.abort();
    question.value = "";
    submittedQuestion.value = "";
    result.value = null;
    history.value = [];
    error.value = "";
    streaming.value = false;
  }

  function cancel(): void {
    activeController?.abort();
  }

  onScopeDispose(() => {
    activeController?.abort();
    if (timeoutHandle) clearTimeout(timeoutHandle);
  });

  return {
    question,
    submittedQuestion,
    result,
    loading,
    streaming,
    error,
    history,
    submit,
    clear,
    cancel,
  };
}
