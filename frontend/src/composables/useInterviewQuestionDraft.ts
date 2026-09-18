import { computed, readonly, shallowRef } from "vue";

import {
  generateInterviewQuestions,
  prepareInterview,
  type InterviewQuestion,
  type InterviewQuestionAllocation,
  type InterviewQuestionDraftPayload,
  type InterviewQuestionGenerationResult,
  type InterviewPreparationResult,
  type InterviewSessionCreatePayload,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

const MIN_CONFIRMED_QUESTION_COUNT = 3;

function fingerprint(payload: InterviewQuestionDraftPayload): string {
  return JSON.stringify([
    payload.targetRole.trim(),
    payload.resumeText.trim(),
    payload.jobDescription?.trim() || "",
    payload.questionCount,
    payload.maxQuestionCount,
    payload.skill,
    payload.difficulty,
  ]);
}

export function useInterviewQuestionDraft() {
  const preparation = shallowRef<InterviewPreparationResult | null>(null);
  const generation = shallowRef<InterviewQuestionGenerationResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const generatedFrom = shallowRef("");

  const questions = computed(() => generation.value?.questions ?? []);
  const canConfirm = computed(() => questions.value.length >= MIN_CONFIRMED_QUESTION_COUNT
    && questions.value.every((question) => Boolean(question.question.trim() && question.objective.trim())));

  async function generate(
    payload: InterviewQuestionDraftPayload,
    currentPayload: () => InterviewQuestionDraftPayload,
  ): Promise<void> {
    if (loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      const nextPreparation = await prepareInterview({
        targetRole: payload.targetRole,
        resumeText: payload.resumeText,
        jobDescription: payload.jobDescription,
      });
      const nextGeneration = await generateInterviewQuestions(
        nextPreparation.targetRole,
        nextPreparation.profile,
        payload.questionCount,
        payload.maxQuestionCount,
        payload.skill,
        payload.difficulty,
        nextPreparation.promptVersion,
      );
      if (fingerprint(payload) !== fingerprint(currentPayload())) {
        return;
      }
      preparation.value = nextPreparation;
      generation.value = nextGeneration;
      generatedFrom.value = fingerprint(payload);
    } catch (requestError) {
      if (fingerprint(payload) === fingerprint(currentPayload())) {
        error.value = resolveApiError(requestError, "生成面试题草稿失败");
      }
    } finally {
      loading.value = false;
    }
  }

  function invalidateIfChanged(payload: InterviewQuestionDraftPayload): void {
    if (generatedFrom.value && generatedFrom.value !== fingerprint(payload)) {
      clear();
    }
  }

  function updateQuestion(index: number, questionText: string): void {
    if (!generation.value || !generation.value.questions[index]) {
      return;
    }
    const current = generation.value.questions[index];
    const nextQuestion: InterviewQuestion = current.question === questionText
      ? current
      : {
          ...current,
          question: questionText,
          knowledgeQuery: questionText.trim(),
          knowledgeGrounded: false,
          knowledgeSources: [],
        };
    generation.value = {
      ...generation.value,
      questions: generation.value.questions.map((question, questionIndex) =>
        questionIndex === index ? nextQuestion : question),
      configuration: {
        ...generation.value.configuration,
        questionAllocations: generation.value.configuration.questionAllocations.map((allocation) =>
          allocation.questionIndex === index
            ? {
                ...allocation,
                supportingSources: allocation.supportingSources.filter(
                  (source) => source !== "KNOWLEDGE",
                ),
              }
            : allocation),
      },
    };
  }

  function removeQuestion(index: number): void {
    if (!generation.value || generation.value.questions.length <= MIN_CONFIRMED_QUESTION_COUNT) {
      return;
    }
    const remainingAllocations = generation.value.configuration.questionAllocations
      .filter((allocation) => allocation.questionIndex !== index)
      .map((allocation, questionIndex) => ({ ...allocation, questionIndex }));
    generation.value = {
      ...generation.value,
      questions: generation.value.questions.filter((_, questionIndex) => questionIndex !== index),
      configuration: updateConfiguration(
        generation.value.configuration,
        remainingAllocations,
      ),
    };
  }

  function updateObjective(index: number, objective: string): void {
    if (!generation.value || !generation.value.questions[index]) {
      return;
    }
    generation.value = {
      ...generation.value,
      questions: generation.value.questions.map((question, questionIndex) =>
        questionIndex === index ? { ...question, objective } : question),
    };
  }

  function createSessionPayload(maxQuestionCount: number): InterviewSessionCreatePayload | null {
    if (!preparation.value || !generation.value || !canConfirm.value) {
      return null;
    }
    return {
      targetRole: preparation.value.targetRole,
      profile: preparation.value.profile,
      questions: generation.value.questions.map((question) => ({
        ...question,
        question: question.question.trim(),
        objective: question.objective.trim(),
        knowledgeQuery: question.knowledgeQuery.trim(),
      })),
      maxQuestionCount: Math.max(maxQuestionCount, generation.value.questions.length),
      configuration: {
        ...generation.value.configuration,
        maxQuestionCount: Math.max(maxQuestionCount, generation.value.questions.length),
      },
    };
  }

  function clear(): void {
    preparation.value = null;
    generation.value = null;
    generatedFrom.value = "";
    error.value = "";
  }

  return {
    preparation: readonly(preparation),
    generation: readonly(generation),
    questions,
    loading: readonly(loading),
    error: readonly(error),
    canConfirm,
    generate,
    invalidateIfChanged,
    updateQuestion,
    updateObjective,
    removeQuestion,
    createSessionPayload,
    clear,
  };
}

function updateConfiguration(
  configuration: InterviewQuestionGenerationResult["configuration"],
  allocations: InterviewQuestionAllocation[],
): InterviewQuestionGenerationResult["configuration"] {
  const sourceQuotas = allocations.reduce<InterviewQuestionGenerationResult["configuration"]["sourceQuotas"]>(
    (quotas, allocation) => ({
      ...quotas,
      [allocation.primarySource]: (quotas[allocation.primarySource] ?? 0) + 1,
    }),
    {},
  );
  return {
    ...configuration,
    initialQuestionCount: allocations.length,
    sourceQuotas,
    questionAllocations: allocations,
  };
}
