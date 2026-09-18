import { computed, readonly, shallowRef } from "vue";

import {
  generateInterviewDraft,
  replaceInterviewDraftQuestion,
  type InterviewDraftResult,
  type InterviewQuestion,
  type InterviewQuestionAllocation,
  type InterviewQuestionDraftPayload,
  type InterviewQuestionGenerationResult,
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
  const draft = shallowRef<InterviewDraftResult | null>(null);
  const loading = shallowRef(false);
  const replacingQuestionIndex = shallowRef<number | null>(null);
  const error = shallowRef("");
  const generatedFrom = shallowRef("");
  const sessionRequestId = shallowRef("");

  const preparation = computed(() => draft.value?.preparation ?? null);
  const generation = computed(() => draft.value?.generation ?? null);
  const questions = computed(() => generation.value?.questions ?? []);
  const busy = computed(() => loading.value || replacingQuestionIndex.value !== null);
  const canConfirm = computed(() => questions.value.length >= MIN_CONFIRMED_QUESTION_COUNT
    && questions.value.every((question) => Boolean(question.question.trim() && question.objective.trim())));

  async function generate(
    payload: InterviewQuestionDraftPayload,
    currentPayload: () => InterviewQuestionDraftPayload,
    regenerate = false,
  ): Promise<void> {
    if (busy.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      const nextDraft = await generateInterviewDraft({ ...payload, regenerate });
      if (fingerprint(payload) !== fingerprint(currentPayload())) {
        return;
      }
      draft.value = nextDraft;
      sessionRequestId.value = crypto.randomUUID();
      generatedFrom.value = fingerprint(payload);
    } catch (requestError) {
      if (fingerprint(payload) === fingerprint(currentPayload())) {
        error.value = resolveApiError(requestError, "生成面试题草稿失败");
      }
    } finally {
      loading.value = false;
    }
  }

  async function replaceQuestion(index: number): Promise<void> {
    if (!draft.value || !generation.value || busy.value) {
      return;
    }
    replacingQuestionIndex.value = index;
    error.value = "";
    try {
      draft.value = await replaceInterviewDraftQuestion(
        draft.value.draftId,
        index,
        generation.value,
      );
    } catch (requestError) {
      error.value = resolveApiError(requestError, "替换面试题失败");
    } finally {
      replacingQuestionIndex.value = null;
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
    updateGeneration({
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
    });
  }

  function removeQuestion(index: number): void {
    if (!generation.value || generation.value.questions.length <= MIN_CONFIRMED_QUESTION_COUNT) {
      return;
    }
    const remainingAllocations = generation.value.configuration.questionAllocations
      .filter((allocation) => allocation.questionIndex !== index)
      .map((allocation, questionIndex) => ({ ...allocation, questionIndex }));
    updateGeneration({
      ...generation.value,
      questions: generation.value.questions.filter((_, questionIndex) => questionIndex !== index),
      configuration: updateConfiguration(
        generation.value.configuration,
        remainingAllocations,
      ),
    });
  }

  function updateObjective(index: number, objective: string): void {
    if (!generation.value || !generation.value.questions[index]) {
      return;
    }
    updateGeneration({
      ...generation.value,
      questions: generation.value.questions.map((question, questionIndex) =>
        questionIndex === index ? { ...question, objective } : question),
    });
  }

  function createSessionPayload(maxQuestionCount: number): InterviewSessionCreatePayload | null {
    if (!preparation.value || !generation.value || !canConfirm.value) {
      return null;
    }
    if (!sessionRequestId.value) {
      sessionRequestId.value = crypto.randomUUID();
    }
    return {
      requestId: sessionRequestId.value,
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

  function updateGeneration(nextGeneration: InterviewQuestionGenerationResult): void {
    if (!draft.value) {
      return;
    }
    draft.value = {
      ...draft.value,
      reused: false,
      actualQuestionCount: nextGeneration.questions.length,
      status: nextGeneration.questions.length === draft.value.targetQuestionCount
        && !nextGeneration.unavailableSources.length ? "COMPLETE" : "PARTIAL",
      generation: nextGeneration,
    };
  }

  function clear(): void {
    draft.value = null;
    sessionRequestId.value = "";
    generatedFrom.value = "";
    error.value = "";
  }

  return {
    draft: readonly(draft),
    preparation,
    generation,
    questions,
    loading: readonly(loading),
    replacingQuestionIndex: readonly(replacingQuestionIndex),
    busy,
    error: readonly(error),
    canConfirm,
    generate,
    replaceQuestion,
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
