import { shallowRef } from "vue";

import {
  answerKnowledgeQuestion,
  type KnowledgeAnswerResponse,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

export const KNOWLEDGE_QUESTION_MAX_LENGTH = 2_000;

export function useKnowledgeAnswer() {
  const question = shallowRef("");
  const submittedQuestion = shallowRef("");
  const result = shallowRef<KnowledgeAnswerResponse | null>(null);
  const loading = shallowRef(false);
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
    error.value = "";
    result.value = null;
    submittedQuestion.value = normalizedQuestion;
    try {
      result.value = await answerKnowledgeQuestion(normalizedQuestion);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "知识问答失败");
    } finally {
      loading.value = false;
    }
  }

  function clear(): void {
    question.value = "";
    submittedQuestion.value = "";
    result.value = null;
    error.value = "";
  }

  return {
    question,
    submittedQuestion,
    result,
    loading,
    error,
    submit,
    clear,
  };
}
