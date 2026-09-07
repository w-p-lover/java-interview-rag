import { shallowRef } from "vue";

import {
  streamKnowledgeQuestion,
  type KnowledgeAnswerResponse,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

export const KNOWLEDGE_QUESTION_MAX_LENGTH = 2_000;

export function useKnowledgeAnswer() {
  const question = shallowRef("");
  const submittedQuestion = shallowRef("");
  const result = shallowRef<KnowledgeAnswerResponse | null>(null);
  const loading = shallowRef(false);
  const streaming = shallowRef(false);
  const error = shallowRef("");

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
        if (event.type === "delta") {
          result.value = {
            answer: `${result.value?.answer ?? ""}${event.content}`,
            grounded: false,
            sources: [],
            claimEvidence: [],
            retrievalStrategy: result.value?.retrievalStrategy ?? "HYBRID",
            degraded: false,
            degradationReason: null,
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
      });
    } catch (requestError) {
      error.value = requestError instanceof Error
        ? requestError.message
        : resolveApiError(requestError, "知识问答失败");
    } finally {
      loading.value = false;
      streaming.value = false;
    }
  }

  function clear(): void {
    question.value = "";
    submittedQuestion.value = "";
    result.value = null;
    error.value = "";
    streaming.value = false;
  }

  return {
    question,
    submittedQuestion,
    result,
    loading,
    streaming,
    error,
    submit,
    clear,
  };
}
