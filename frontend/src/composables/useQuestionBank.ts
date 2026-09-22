import { computed, onMounted, onUnmounted, reactive, shallowRef, watch } from "vue";

import { listKnowledgeDocuments, type KnowledgeDocumentCatalogItem } from "@/api/knowledge";
import type { InterviewDifficulty, InterviewSkill } from "@/api/interview";
import { resolveApiError } from "@/api/request";
import {
  cancelBankGeneration, changeBankQuestionStatus, createBankQuestion, createBankSession, deleteBankQuestion,
  getSkillTrainingPlan,
  listBankGenerationTasks, listBankQuestions, resumeBankGeneration, submitBankGeneration,
  updateBankQuestion,
  type BankQuestion, type BankQuestionContent, type BankQuestionStatus, type GenerationTask,
  type SkillTrainingPlan,
} from "@/api/questionBank";

export function useQuestionBank(
  initialSource = "",
  initialSkill: InterviewSkill = "JAVA_CORE",
  initialFocusDimension = "",
) {
  const documents = shallowRef<KnowledgeDocumentCatalogItem[]>([]);
  const source = shallowRef(initialSource);
  const config = reactive({
    skill: initialSkill,
    difficulty: "INTERMEDIATE" as InterviewDifficulty,
    questionCount: 3,
    followUpCount: 0,
  });
  const filters = reactive({ status: "" as BankQuestionStatus | "", difficulty: "" as InterviewDifficulty | "", category: "", keyword: "" });
  const focusDimension = shallowRef(initialFocusDimension);
  const trainingPlan = shallowRef<SkillTrainingPlan | null>(null);
  const trainingPlanLoading = shallowRef(false);
  const trainingPlanError = shallowRef("");
  const questions = shallowRef<BankQuestion[]>([]);
  const tasks = shallowRef<GenerationTask[]>([]);
  const selectedIds = shallowRef<string[]>([]);
  const loading = shallowRef(false);
  const documentsLoading = shallowRef(false);
  const listError = shallowRef("");
  const taskError = shallowRef("");
  const documentError = shallowRef("");
  const actionError = shallowRef("");
  const actionKey = shallowRef("");
  const generationSubmitted = shallowRef(false);
  const generationRequestId = shallowRef("");
  const generationPending = shallowRef(false);
  const sessionRequestId = shallowRef("");
  const currentDocument = computed(() => documents.value.find((document) => document.source === source.value));
  const busy = computed(() => Boolean(actionKey.value));
  const activeTasks = computed(() => tasks.value.some(isRunning));
  const eligibleQuestions = computed(() => questions.value.filter(isEligible));
  const selectedQuestions = computed(() => eligibleQuestions.value.filter((question) => selectedIds.value.includes(question.id)));
  const validSessionCapacity = computed(() => config.questionCount >= 3 && config.questionCount <= 8
    && config.questionCount * (1 + config.followUpCount) <= 8);
  const canStartSelected = computed(() => validSessionCapacity.value && selectedQuestions.value.length === config.questionCount
    && selectedQuestions.value.every((question) => question.followUps.length >= config.followUpCount));
  const canStartRandom = computed(() => {
    if (!validSessionCapacity.value || !currentDocument.value) return false;
    const candidates = eligibleQuestions.value.filter((question) => question.followUps.length >= config.followUpCount);
    if (filters.category.trim()) return candidates.length >= config.questionCount;
    const plan = trainingPlan.value;
    if (!plan) return false;
    return plan.categories.every((quota) =>
      candidates.filter((question) => question.category === quota.category).length >= quota.questionCount);
  });

  let disposed = false;
  let sessionMode: "selected" | "random" | null = null;
  let sourceEpoch = 0;
  let questionController: AbortController | null = null;
  let taskController: AbortController | null = null;
  let trainingPlanController: AbortController | null = null;
  let pollTimer: ReturnType<typeof setTimeout> | undefined;
  let filterTimer: ReturnType<typeof setTimeout> | undefined;

  function isEligible(question: BankQuestion): boolean {
    return question.status === "ACTIVE" && question.documentVersion === currentDocument.value?.version
      && question.skill === config.skill && question.difficulty === config.difficulty;
  }

  async function loadDocuments(): Promise<void> {
    documentsLoading.value = true;
    documentError.value = "";
    try {
      const result = await listKnowledgeDocuments();
      if (disposed) return;
      documents.value = result.documents;
      if (!source.value) source.value = result.documents[0]?.source ?? "";
    } catch (error) {
      if (!disposed) documentError.value = resolveApiError(error, "读取知识资料失败");
    } finally {
      if (!disposed) documentsLoading.value = false;
    }
  }

  async function loadQuestions(): Promise<void> {
    questionController?.abort();
    if (!source.value || disposed) return;
    const controller = new AbortController();
    questionController = controller;
    loading.value = true;
    listError.value = "";
    try {
      const result = await listBankQuestions({ source: source.value, ...filters }, controller.signal);
      if (controller.signal.aborted || disposed) return;
      questions.value = result;
      const nextSelectedIds = selectedIds.value.filter((id) => result.some((question) => question.id === id && isEligible(question)));
      if (nextSelectedIds.length !== selectedIds.value.length) selectedIds.value = nextSelectedIds;
    } catch (error) {
      if (!controller.signal.aborted && !disposed) listError.value = resolveApiError(error, "读取题库失败");
    } finally {
      if (questionController === controller && !disposed) loading.value = false;
    }
  }

  function schedulePoll(): void {
    clearTimeout(pollTimer);
    if (!disposed && activeTasks.value) pollTimer = setTimeout(() => void loadTasks(), 2000);
  }

  async function loadTasks(): Promise<void> {
    clearTimeout(pollTimer);
    taskController?.abort();
    if (!source.value || disposed) return;
    const controller = new AbortController();
    taskController = controller;
    taskError.value = "";
    try {
      const result = await listBankGenerationTasks(source.value, controller.signal);
      if (controller.signal.aborted || disposed) return;
      const changed = result.some((task) => {
        const previous = tasks.value.find((item) => item.id === task.id);
        return task.savedCount > (previous?.savedCount ?? 0) || (previous && previous.status !== task.status);
      });
      tasks.value = result;
      if (changed) void loadQuestions();
    } catch (error) {
      if (!controller.signal.aborted && !disposed) taskError.value = resolveApiError(error, "读取出题任务失败");
    } finally {
      if (taskController === controller && !controller.signal.aborted) schedulePoll();
    }
  }

  async function loadTrainingPlan(): Promise<void> {
    trainingPlanController?.abort();
    if (disposed) return;
    const controller = new AbortController();
    trainingPlanController = controller;
    trainingPlanLoading.value = true;
    trainingPlanError.value = "";
    try {
      const result = await getSkillTrainingPlan(
        config.skill,
        config.questionCount,
        focusDimension.value.trim() || undefined,
        controller.signal,
      );
      if (!controller.signal.aborted && !disposed) trainingPlan.value = result;
    } catch (error) {
      if (!controller.signal.aborted && !disposed) {
        trainingPlan.value = null;
        trainingPlanError.value = resolveApiError(error, "读取分类配额失败");
      }
    } finally {
      if (trainingPlanController === controller && !disposed) trainingPlanLoading.value = false;
    }
  }

  async function generate(newRequest = false): Promise<void> {
    if (busy.value || !currentDocument.value) return;
    if (newRequest || !generationRequestId.value) generationRequestId.value = crypto.randomUUID();
    const payload = { ...config, source: source.value, requestId: generationRequestId.value };
    const epoch = sourceEpoch;
    actionKey.value = "generate";
    actionError.value = "";
    generationPending.value = true;
    try {
      const task = await submitBankGeneration(payload);
      if (epoch !== sourceEpoch || disposed) return;
      generationSubmitted.value = true;
      generationPending.value = false;
      tasks.value = [task, ...tasks.value.filter((item) => item.id !== task.id)].slice(0, 20);
      await loadTasks();
      void loadQuestions();
    } catch (error) {
      if (epoch === sourceEpoch && !disposed) actionError.value = resolveApiError(error, "提交出题任务失败");
    } finally {
      if (!disposed) actionKey.value = "";
    }
  }

  async function resumeTask(task: GenerationTask): Promise<void> {
    const next = await perform(`resume:${task.id}`, "恢复出题任务失败", () => resumeBankGeneration(task.id));
    if (next) {
      tasks.value = tasks.value.map((item) => item.id === next.id ? next : item);
      await loadTasks();
    }
  }

  async function cancelTask(task: GenerationTask): Promise<void> {
    const next = await perform(`cancel:${task.id}`, "取消出题任务失败", () => cancelBankGeneration(task.id));
    if (next) {
      tasks.value = tasks.value.map((item) => item.id === next.id ? next : item);
      await loadTasks();
    }
  }

  async function perform<T>(key: string, message: string, action: () => Promise<T>): Promise<T | null> {
    if (busy.value) return null;
    const epoch = sourceEpoch;
    actionKey.value = key;
    actionError.value = "";
    try {
      const result = await action();
      if (epoch !== sourceEpoch || disposed) return null;
      return result;
    } catch (error) {
      if (epoch === sourceEpoch && !disposed) actionError.value = resolveApiError(error, message);
      return null;
    } finally {
      if (!disposed) actionKey.value = "";
    }
  }

  async function saveQuestion(content: BankQuestionContent, editing: BankQuestion | null): Promise<BankQuestion | null> {
    const result = await perform("save", "保存题目失败", () => editing
      ? updateBankQuestion(editing.id, editing.version, content)
      : createBankQuestion({ ...content, source: source.value, skill: config.skill, difficulty: config.difficulty }));
    if (result) await loadQuestions();
    return result;
  }

  async function setStatus(question: BankQuestion, status: Exclude<BankQuestionStatus, "STALE">): Promise<void> {
    const result = await perform(`status:${question.id}`, "更新题目状态失败", () => changeBankQuestionStatus(question.id, question.version, status));
    if (result) await loadQuestions();
  }

  async function removeQuestion(question: BankQuestion): Promise<void> {
    if (!window.confirm(`删除题目“${question.question}”？`)) return;
    const result = await perform(`delete:${question.id}`, "删除题目失败", async () => {
      await deleteBankQuestion(question.id, question.version);
      return true;
    });
    if (result) await loadQuestions();
  }

  function toggleSelection(id: string): void {
    selectedIds.value = selectedIds.value.includes(id)
      ? selectedIds.value.filter((value) => value !== id) : [...selectedIds.value, id];
  }

  async function startSession(random = false): Promise<string | null> {
    if (!currentDocument.value || !validSessionCapacity.value || (!random && !canStartSelected.value)) return null;
    const mode = random ? "random" : "selected";
    if (sessionMode !== mode) sessionRequestId.value = "";
    sessionMode = mode;
    if (!sessionRequestId.value) sessionRequestId.value = crypto.randomUUID();
    const result = await perform("session", "创建专项面试失败", () => createBankSession({
      ...config, requestId: sessionRequestId.value, source: source.value,
      questionIds: random ? [] : [...selectedIds.value], category: filters.category.trim() || undefined,
      focusDimension: random && !filters.category.trim()
        ? focusDimension.value.trim() || undefined : undefined,
    }));
    return result?.id ?? null;
  }

  watch(source, () => {
    sourceEpoch += 1;
    clearTimeout(pollTimer);
    clearTimeout(filterTimer);
    questionController?.abort();
    taskController?.abort();
    questions.value = [];
    tasks.value = [];
    selectedIds.value = [];
    listError.value = "";
    taskError.value = "";
    actionError.value = "";
    generationRequestId.value = "";
    generationSubmitted.value = false;
    generationPending.value = false;
    sessionRequestId.value = "";
    loading.value = false;
    void loadQuestions();
    void loadTasks();
  }, { immediate: true });

  watch(() => [config.skill, config.difficulty, config.questionCount, config.followUpCount], () => {
    generationRequestId.value = "";
    generationSubmitted.value = false;
    generationPending.value = false;
    sessionRequestId.value = "";
    selectedIds.value = selectedIds.value.filter((id) => eligibleQuestions.value.some((question) => question.id === id));
  }, { flush: "sync" });
  watch(() => config.skill, (value, previous) => {
    if (previous && value !== previous) focusDimension.value = "";
  }, { flush: "sync" });
  watch(() => [config.skill, config.questionCount, focusDimension.value], () => {
    void loadTrainingPlan();
  }, { immediate: true });
  watch(() => filters.category, (category) => {
    if (category.trim() && focusDimension.value) focusDimension.value = "";
  });

  watch(() => [filters.status, filters.difficulty, filters.category, filters.keyword], () => {
    questionController?.abort();
    clearTimeout(filterTimer);
    filterTimer = setTimeout(() => void loadQuestions(), 250);
  });
  watch(() => [source.value, config.skill, config.difficulty, config.questionCount, config.followUpCount, filters.category, selectedIds.value.join(",")], () => {
    sessionRequestId.value = "";
  }, { flush: "sync" });

  onMounted(loadDocuments);
  onUnmounted(() => {
    disposed = true;
    clearTimeout(pollTimer);
    clearTimeout(filterTimer);
    questionController?.abort();
    taskController?.abort();
    trainingPlanController?.abort();
  });

  return {
    documents, source, config, filters, questions, tasks, selectedIds, currentDocument,
    loading, documentsLoading, documentError, listError, taskError, actionError, actionKey,
    busy, activeTasks, generationSubmitted, generationPending, eligibleQuestions, selectedQuestions,
    focusDimension, trainingPlan, trainingPlanLoading, trainingPlanError,
    validSessionCapacity, canStartSelected, canStartRandom, isEligible,
    loadDocuments, loadQuestions, loadTasks, loadTrainingPlan,
    generate, resumeTask, cancelTask, saveQuestion, setStatus, removeQuestion, toggleSelection, startSession,
  };
}

function isRunning(task: GenerationTask): boolean {
  return task.status === "QUEUED" || task.status === "PROCESSING";
}
