<script setup lang="ts">
import { computed, nextTick, shallowRef, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowRight, Plus, RefreshCw, Shuffle, Sparkles } from "@lucide/vue";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import QuestionBankEditor from "@/components/interview/QuestionBankEditor.vue";
import QuestionBankList from "@/components/interview/QuestionBankList.vue";
import QuestionBankTrainingPlan from "@/components/interview/QuestionBankTrainingPlan.vue";
import { useQuestionBank } from "@/composables/useQuestionBank";
import type { BankQuestion, BankQuestionContent, GenerationTask } from "@/api/questionBank";
import type { InterviewDifficulty, InterviewSkill } from "@/api/interview";

const route = useRoute();
const router = useRouter();
const skillOptions: Array<{ value: InterviewSkill; label: string }> = [
  { value: "JAVA_CORE", label: "Java 核心" }, { value: "SPRING", label: "Spring" }, { value: "DATABASE", label: "数据库" },
  { value: "SYSTEM_DESIGN", label: "系统设计" }, { value: "RAG_AI", label: "RAG / AI" },
];
const initialSkill = skillOptions.some((option) => option.value === route.query.skill)
  ? route.query.skill as InterviewSkill : "JAVA_CORE";
const bank = useQuestionBank(
  typeof route.query.source === "string" ? route.query.source : "",
  initialSkill,
  typeof route.query.focusDimension === "string" ? route.query.focusDimension : "",
);
const {
  source, config, filters, documents, currentDocument, questions, tasks, selectedIds,
  loading, documentsLoading, documentError, listError, taskError, actionError, actionKey,
  busy, generationSubmitted, generationPending, eligibleQuestions, selectedQuestions,
  focusDimension, trainingPlan, trainingPlanLoading, trainingPlanError,
  validSessionCapacity, canStartSelected, canStartRandom,
} = bank;
const editing = shallowRef<BankQuestion | null>(null);
const editorOpen = shallowRef(false);
const editorError = shallowRef("");
const eligibleIds = computed(() => eligibleQuestions.value.map((question) => question.id));
const difficultyOptions: Array<{ value: InterviewDifficulty; label: string }> = [
  { value: "BASIC", label: "基础" }, { value: "INTERMEDIATE", label: "进阶" }, { value: "ADVANCED", label: "高级场景" },
];
const taskLabels: Record<GenerationTask["status"], string> = { QUEUED: "已排队", PROCESSING: "生成中", COMPLETED: "已完成", FAILED: "失败" };

watch([source, () => config.skill, focusDimension], ([nextSource, skill, focus]) => {
  editorOpen.value = false;
  editing.value = null;
  void router.replace({ query: {
    ...route.query,
    source: nextSource || undefined,
    skill,
    focusDimension: focus || undefined,
  } });
});
watch(() => [route.query.source, route.query.skill, route.query.focusDimension], ([nextSource, nextSkill, nextFocus]) => {
  if (typeof nextSource === "string" && nextSource !== source.value) source.value = nextSource;
  if (skillOptions.some((option) => option.value === nextSkill) && nextSkill !== config.skill) {
    config.skill = nextSkill as InterviewSkill;
  }
  const normalizedFocus = typeof nextFocus === "string" ? nextFocus : "";
  if (normalizedFocus !== focusDimension.value) focusDimension.value = normalizedFocus;
});

async function openEditor(question: BankQuestion | null): Promise<void> {
  editing.value = question;
  editorError.value = "";
  editorOpen.value = true;
  await nextTick();
  const editor = document.getElementById("bank-editor");
  editor?.scrollIntoView({ block: "start" });
  editor?.focus({ preventScroll: true });
}

async function save(content: BankQuestionContent): Promise<void> {
  const result = await bank.saveQuestion(content, editing.value);
  if (result) {
    editing.value = result;
    editorError.value = "";
  } else {
    editorError.value = actionError.value;
  }
}

async function start(random: boolean): Promise<void> {
  const sessionId = await bank.startSession(random);
  if (sessionId) await router.push({ name: "interview", query: { sessionId } });
}

function refresh(): void {
  void bank.loadDocuments();
  void bank.loadQuestions();
  void bank.loadTasks();
}
</script>

<template>
  <WorkspaceHeader mode="question-bank" />
  <main id="main-content" class="question-bank" tabindex="-1">
    <header class="bank-heading"><h1>知识题库</h1><button type="button" class="icon-action" :disabled="busy || loading" aria-label="刷新题库" title="刷新题库" @click="refresh"><RefreshCw :size="19" aria-hidden="true" /></button></header>

    <section id="bank-configuration" class="configuration" aria-label="资料与出题配置" tabindex="-1">
      <div class="field source-field"><label for="bank-source">知识资料</label><select id="bank-source" v-model="source" :disabled="documentsLoading || busy"><option value="" disabled>选择知识资料</option><option v-if="source && !currentDocument" :value="source">{{ source }}（不可用）</option><option v-for="document in documents" :key="document.source" :value="document.source">{{ document.source }}</option></select></div>
      <div class="field"><label for="bank-skill">Skill</label><select id="bank-skill" v-model="config.skill" :disabled="busy"><option v-for="option in skillOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></div>
      <div class="field"><label for="bank-difficulty">出题 / 面试难度</label><select id="bank-difficulty" v-model="config.difficulty" :disabled="busy"><option v-for="option in difficultyOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></div>
      <div class="field"><label for="bank-count">首轮题量</label><select id="bank-count" v-model="config.questionCount" :disabled="busy"><option v-for="count in 8" :key="count" :value="count">{{ count }} 道</option></select></div>
      <div class="field"><label for="bank-follow-count">每题固定追问</label><select id="bank-follow-count" v-model="config.followUpCount" :disabled="busy"><option v-for="count in [0, 1, 2]" :key="count" :value="count">{{ count }} 道</option></select></div>
      <QuestionBankTrainingPlan v-model:focus="focusDimension" :plan="trainingPlan" :loading="trainingPlanLoading" :busy="busy" />
      <div class="configuration-footer">
        <span v-if="currentDocument" class="document-version">当前版本 {{ currentDocument.version }}</span>
        <RouterLink v-else-if="!documentsLoading && !documents.length" to="/" class="document-link">导入知识资料</RouterLink>
        <div class="configuration-actions">
          <button v-if="!generationSubmitted" type="button" class="primary-action" :disabled="busy || !currentDocument" @click="bank.generate()"><Sparkles :size="17" aria-hidden="true" />{{ actionKey === 'generate' ? '正在提交…' : generationPending ? '重试提交' : '生成题目' }}</button>
          <button type="button" :class="generationSubmitted ? 'primary-action' : 'secondary-action'" :disabled="busy || !currentDocument" @click="bank.generate(true)"><RefreshCw :size="17" aria-hidden="true" />新生成</button>
        </div>
      </div>
    </section>
    <p v-if="documentError" class="error-message" role="alert">{{ documentError }}</p>
    <p v-if="trainingPlanError" class="error-message" role="alert">{{ trainingPlanError }}</p>
    <p v-if="actionError && !editorError" class="error-message" role="alert">{{ actionError }}</p>

    <section id="bank-tasks" class="task-section" aria-labelledby="bank-tasks-title" tabindex="-1">
      <header class="section-heading"><h2 id="bank-tasks-title">最近出题任务</h2><button type="button" class="icon-action" :disabled="busy || !source" aria-label="刷新出题任务" title="刷新出题任务" @click="bank.loadTasks()"><RefreshCw :size="17" aria-hidden="true" /></button></header>
      <p v-if="taskError" class="error-message" role="alert">{{ taskError }}</p>
      <p v-if="!tasks.length" class="empty-state">暂无出题任务。</p>
      <ul v-else class="task-list">
        <li v-for="task in tasks" :key="task.id" class="task-row">
          <div class="task-main"><strong class="task-status" :data-status="task.status">{{ taskLabels[task.status] }}</strong><span>{{ task.savedCount }} / {{ task.questionCount }} 道已保存<template v-if="task.skippedCount"> · 跳过 {{ task.skippedCount }} 道</template></span><span>{{ difficultyOptions.find(option => option.value === task.difficulty)?.label }} · 追问 {{ task.followUpCount }} 道</span></div>
          <div class="task-details"><span>{{ task.createdAt.replace('T', ' ').slice(0, 16) }} · 版本 {{ task.documentVersion }}</span><span v-if="task.recoveryCount">已自动恢复 {{ task.recoveryCount }} 次</span><span v-if="task.status === 'PROCESSING' && task.lastHeartbeatAt">最近心跳 {{ task.lastHeartbeatAt.replace('T', ' ').slice(11, 19) }}</span><span v-if="task.errorCode" class="task-failure">{{ task.errorCode === 'RECOVERY_EXHAUSTED' ? 'Worker 连续失联，已停止自动恢复' : task.errorCode }}</span></div>
          <button v-if="task.status === 'FAILED'" type="button" class="secondary-action resume-action" :disabled="busy || task.documentVersion !== currentDocument?.version" :title="task.documentVersion !== currentDocument?.version ? '资料版本失效' : '恢复未完成题目'" @click="bank.resumeTask(task)"><RefreshCw :size="16" aria-hidden="true" />{{ actionKey === `resume:${task.id}` ? '正在恢复…' : '恢复' }}</button>
        </li>
      </ul>
    </section>

    <section id="bank-questions" class="questions-section" aria-labelledby="bank-questions-title" tabindex="-1">
      <header class="section-heading"><h2 id="bank-questions-title">题目 · {{ questions.length }}</h2><button type="button" class="secondary-action" :disabled="busy || !currentDocument" @click="openEditor(null)"><Plus :size="17" aria-hidden="true" />手工草稿</button></header>
      <div class="filters">
        <div class="field"><label for="bank-status-filter">状态</label><select id="bank-status-filter" v-model="filters.status" :disabled="busy"><option value="">全部状态</option><option value="DRAFT">草稿</option><option value="ACTIVE">已审核</option><option value="ARCHIVED">已停用</option><option value="STALE">待复核</option></select></div>
        <div class="field"><label for="bank-difficulty-filter">题目难度</label><select id="bank-difficulty-filter" v-model="filters.difficulty" :disabled="busy"><option value="">全部难度</option><option v-for="option in difficultyOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></div>
        <div class="field"><label for="bank-category-filter">分类</label><input id="bank-category-filter" v-model="filters.category" :disabled="busy" type="search" maxlength="100" /></div>
        <div class="field"><label for="bank-keyword-filter">关键词</label><input id="bank-keyword-filter" v-model="filters.keyword" :disabled="busy" type="search" maxlength="200" /></div>
      </div>
      <p v-if="listError" class="error-message" role="alert">{{ listError }}</p>
      <QuestionBankList :questions="questions" :selected-ids="selectedIds" :eligible-ids="eligibleIds" :current-version="currentDocument?.version ?? ''" :busy="busy" :loading="loading" @select="bank.toggleSelection" @edit="openEditor" @status="bank.setStatus" @remove="bank.removeQuestion" />
      <footer class="session-actions"><span>已选 {{ selectedQuestions.length }} / {{ config.questionCount }} 道 · 可用 {{ eligibleQuestions.length }} 道</span><div><button type="button" class="secondary-action" :disabled="busy || loading || !canStartRandom" @click="start(true)"><Shuffle :size="17" aria-hidden="true" />随机组卷</button><button type="button" class="primary-action" :disabled="busy || loading || !canStartSelected" @click="start(false)"><ArrowRight :size="17" aria-hidden="true" />{{ actionKey === 'session' ? '正在创建…' : '开始面试' }}</button></div></footer>
      <p v-if="!validSessionCapacity" class="capacity-message" role="status">正式面试首轮为 3–8 题，含固定追问最多 8 题。</p>
    </section>

    <QuestionBankEditor v-if="editorOpen" :key="editing?.id ?? 'new'" :question="editing" :busy="busy" :error="editorError" @save="save" @close="editorOpen = false; editorError = ''" />
  </main>
</template>

<style scoped lang="scss">
.question-bank { width: min(calc(100% - (var(--space-xl) * 2)), 78rem); margin-inline: auto; padding-block: var(--space-xl) var(--space-3xl); }
.bank-heading, .section-heading, .configuration-footer, .configuration-actions, .session-actions, .session-actions > div { display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm); }
.bank-heading { padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.bank-heading h1 { margin: 0; font-family: var(--font-body); font-size: var(--text-2xl); letter-spacing: 0; }
.section-heading h2 { margin: 0; font-family: var(--font-body); font-size: var(--text-lg); }
.configuration { display: grid; grid-template-columns: minmax(12rem, 2fr) repeat(2, minmax(9rem, 1fr)) repeat(2, minmax(7rem, 0.7fr)); gap: var(--space-md); padding-block: var(--space-lg); }
.field { display: grid; align-content: start; gap: var(--space-xs); min-width: 0; }
.field label { color: var(--color-ink-2); font-size: var(--text-xs); font-weight: 650; }
.field select, .field input { width: 100%; min-width: 0; height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); font-size: var(--text-sm); }
.configuration-footer { grid-column: 1 / -1; flex-wrap: wrap; }
.document-version, .session-actions > span { color: var(--color-muted); font-size: var(--text-sm); overflow-wrap: anywhere; }
.configuration-actions { margin-left: auto; flex-wrap: wrap; }
.document-link { color: var(--color-accent); }
.primary-action, .secondary-action, .icon-action { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-xs); min-height: 2.75rem; padding: var(--space-xs) var(--space-md); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); font-size: var(--text-sm); font-weight: 650; white-space: nowrap; }
.primary-action { border-color: var(--color-accent); color: var(--color-accent-ink); background: var(--color-accent); }
.icon-action { width: 2.75rem; padding: 0; }
.secondary-action:not(:disabled):hover, .icon-action:not(:disabled):hover { color: var(--color-accent); border-color: var(--color-accent); }
.primary-action:not(:disabled):hover { background: var(--color-accent-strong); }
.task-section, .questions-section { padding-block: var(--space-lg); border-top: var(--rule-thin) solid var(--color-rule); }
.task-list { margin: var(--space-md) 0 0; padding: 0; list-style: none; }
.task-row { position: relative; padding-block: var(--space-md); border-bottom: var(--rule-thin) solid var(--color-rule); }
.task-main { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-md); padding-right: 6rem; color: var(--color-ink-2); font-size: var(--text-sm); }
.task-status { color: var(--color-accent); }
.task-status[data-status="COMPLETED"] { color: var(--color-success); }
.task-status[data-status="FAILED"], .task-failure { color: var(--color-error); }
.task-details { display: flex; flex-wrap: wrap; gap: var(--space-sm); margin-top: var(--space-xs); padding-right: 6rem; color: var(--color-muted); font-size: var(--text-xs); overflow-wrap: anywhere; }
.resume-action { position: absolute; top: var(--space-md); right: 0; }
.filters { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-md); margin-top: var(--space-lg); }
.session-actions { flex-wrap: wrap; padding-top: var(--space-lg); }
.error-message { padding: var(--space-sm) var(--space-md); border: var(--rule-thin) solid var(--color-error-rule); color: var(--color-error); background: var(--color-error-soft); font-size: var(--text-sm); overflow-wrap: anywhere; }
.empty-state, .capacity-message { margin-bottom: 0; color: var(--color-muted); font-size: var(--text-sm); }
@media (max-width: 68rem) { .configuration { grid-template-columns: repeat(2, minmax(0, 1fr)); } .source-field { grid-column: 1 / -1; } }
@media (max-width: 48rem) { .question-bank { width: calc(100% - (var(--space-md) * 2)); padding-top: var(--space-lg); } .filters { grid-template-columns: repeat(2, minmax(0, 1fr)); } .session-actions > div { width: 100%; } .session-actions > div > button { flex: 1; padding-inline: var(--space-sm); } }
</style>
