<script setup lang="ts">
import { onMounted, shallowRef } from "vue";
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import { createKnowledgeReviewTaskFromFeedback, createRagEvaluationCandidate, evaluateRagEvaluationCandidate, getKnowledgeAnswerFeedback, getKnowledgeAnswerFeedbackDiagnosis, getRagEvaluationCandidates, reviewKnowledgeAnswerFeedback, reviewRagEvaluationCandidate, type KnowledgeAnswerFeedback, type KnowledgeFeedbackDiagnosis, type RagEvaluationCandidate, type RagEvaluationCandidateResult } from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

const items = shallowRef<KnowledgeAnswerFeedback[]>([]);
const loading = shallowRef(false);
const error = shallowRef("");
const selectedStatus = shallowRef("PENDING");
const notes = shallowRef<Record<number, string>>({});
const layers = shallowRef<Record<number, string>>({});
const diagnoses = shallowRef<Record<number, KnowledgeFeedbackDiagnosis>>({});
const taskQuestions = shallowRef<Record<number, string>>({});
const taskClaims = shallowRef<Record<number, string>>({});
const taskStatuses = shallowRef<Record<number, string>>({});
const taskSources = shallowRef<Record<number, string>>({});
const taskIds = shallowRef<Record<number, string>>({});
const candidateQuestions = shallowRef<Record<number, string>>({});
const candidateSources = shallowRef<Record<number, string>>({});
const candidatePoints = shallowRef<Record<number, string>>({});
const candidateIds = shallowRef<Record<number, string>>({});
const candidates = shallowRef<RagEvaluationCandidate[]>([]);
const candidateResults = shallowRef<Record<string, RagEvaluationCandidateResult>>({});

async function load(): Promise<void> {
  loading.value = true;
  error.value = "";
  try { items.value = await getKnowledgeAnswerFeedback(50, selectedStatus.value); }
  catch (requestError) { error.value = resolveApiError(requestError, "读取反馈失败"); }
  finally { loading.value = false; }
}

async function loadCandidates(): Promise<void> {
  try { candidates.value = await getRagEvaluationCandidates(); }
  catch (requestError) { error.value = resolveApiError(requestError, "读取回归候选失败"); }
}

async function diagnose(item: KnowledgeAnswerFeedback): Promise<void> {
  try { diagnoses.value = { ...diagnoses.value, [item.id]: await getKnowledgeAnswerFeedbackDiagnosis(item.id) }; }
  catch (requestError) { error.value = resolveApiError(requestError, "读取召回诊断失败"); }
}

async function review(item: KnowledgeAnswerFeedback, status: "EVALUATION" | "IGNORED"): Promise<void> {
  try {
    const updated = await reviewKnowledgeAnswerFeedback(item.id, status, notes.value[item.id] || "", layers.value[item.id] || null);
    items.value = items.value.map(current => current.id === updated.id ? updated : current);
  } catch (requestError) { error.value = resolveApiError(requestError, "更新反馈审阅状态失败"); }
}

async function createReviewTask(item: KnowledgeAnswerFeedback): Promise<void> {
  const question = taskQuestions.value[item.id]?.trim();
  const claim = taskClaims.value[item.id]?.trim();
  const evidenceStatus = taskStatuses.value[item.id] as "EVIDENCE_INSUFFICIENT" | "EVIDENCE_CONFLICTING" | "CITATION_INVALID" | undefined;
  if (!question || !claim || !evidenceStatus) {
    error.value = "请补充脱敏问题、待纠正断言和证据问题类型";
    return;
  }
  try {
    const sourceNumbers = (taskSources.value[item.id] || "").split(",").map(value => Number(value.trim())).filter(Number.isInteger).filter(value => value > 0);
    const task = await createKnowledgeReviewTaskFromFeedback(item.id, { question, claim, evidenceStatus, sourceNumbers });
    taskIds.value = { ...taskIds.value, [item.id]: task.id };
  } catch (requestError) { error.value = resolveApiError(requestError, "创建复习任务失败"); }
}

async function createCandidate(item: KnowledgeAnswerFeedback): Promise<void> {
  const question = candidateQuestions.value[item.id]?.trim();
  const expectedAnswerPoints = (candidatePoints.value[item.id] || "").split("\n").map(value => value.trim()).filter(Boolean);
  const expectedSources = (candidateSources.value[item.id] || "").split(",").map(value => value.trim()).filter(Boolean);
  if (!question || expectedAnswerPoints.length === 0) {
    error.value = "请补充脱敏问题和至少一个预期回答要点";
    return;
  }
  try {
    const candidate = await createRagEvaluationCandidate(item.id, { question, expectedSources, expectedAnswerPoints });
    candidateIds.value = { ...candidateIds.value, [item.id]: candidate.id };
    await loadCandidates();
  } catch (requestError) { error.value = resolveApiError(requestError, "创建回归候选失败"); }
}

async function reviewCandidate(candidate: RagEvaluationCandidate, status: "ACTIVE" | "IGNORED"): Promise<void> {
  try {
    const updated = await reviewRagEvaluationCandidate(candidate.id, status);
    candidates.value = candidates.value.map(item => item.id === updated.id ? updated : item);
  } catch (requestError) { error.value = resolveApiError(requestError, "更新回归候选状态失败"); }
}

async function evaluateCandidate(candidate: RagEvaluationCandidate): Promise<void> {
  try { candidateResults.value = { ...candidateResults.value, [candidate.id]: await evaluateRagEvaluationCandidate(candidate.id) }; }
  catch (requestError) { error.value = resolveApiError(requestError, "运行回归候选失败"); }
}

onMounted(() => { void load(); void loadCandidates(); });
</script>

<template>
  <WorkspaceHeader mode="observability" />
  <main class="feedback-workspace" tabindex="-1">
    <header class="feedback-heading">
      <div><span>RAG 质量闭环</span><h1>回答反馈审阅</h1></div>
      <select v-model="selectedStatus" aria-label="反馈审阅状态" @change="load">
        <option value="PENDING">待审阅</option><option value="EVALUATION">已纳入评估</option><option value="IGNORED">已忽略</option>
      </select>
    </header>
    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在读取低敏反馈…</div>
    <div v-else-if="items.length" class="feedback-list">
      <article v-for="item in items" :key="item.id" class="feedback-card">
        <header><strong :class="item.feedback === 'NOT_HELPFUL' ? 'danger' : 'helpful'">{{ item.feedback === "NOT_HELPFUL" ? "没帮助" : "有帮助" }}</strong><span>{{ item.retrievalStrategy }} · {{ item.grounded ? "grounded" : "not grounded" }}</span></header>
        <p>问题标识：<code>{{ item.questionHash }}</code></p>
        <textarea v-model="notes[item.id]" maxlength="500" rows="2" placeholder="记录人工判断或后续评估方向"></textarea>
        <button type="button" class="diagnose-button" @click="diagnose(item)">查看最近召回诊断</button>
        <div v-if="diagnoses[item.id]" class="diagnosis">
          <p v-for="log in diagnoses[item.id].retrievalLogs" :key="`${item.id}-${log.id}`">{{ log.status }} · {{ log.sources.length }} 个来源 · {{ log.elapsedMillis }} ms{{ log.errorType ? ` · ${log.errorType}` : "" }}</p>
        </div>
        <select v-if="item.reviewStatus === 'PENDING'" v-model="layers[item.id]" aria-label="人工确认 RAG 失败层"><option value="">选择失败层（可选）</option><option value="RETRIEVAL_MISSING">候选未召回</option><option value="RANKING_LOW">排序过低</option><option value="EVIDENCE_INSUFFICIENT">证据不足</option><option value="ANSWER_UNUSED_EVIDENCE">回答未使用证据</option><option value="UNKNOWN">暂无法判断</option></select>
        <div v-if="item.reviewStatus === 'PENDING'" class="actions"><button type="button" @click="review(item, 'EVALUATION')">纳入评估</button><button type="button" class="secondary" @click="review(item, 'IGNORED')">忽略</button></div>
        <small v-else>已处理：{{ item.reviewStatus === "EVALUATION" ? "纳入评估" : "忽略" }}{{ item.reviewNote ? ` · ${item.reviewNote}` : "" }}</small>
        <div v-if="item.reviewStatus === 'EVALUATION' && item.feedback === 'NOT_HELPFUL'" class="review-task-form">
          <strong>转为针对性复习</strong>
          <p>只录入脱敏问题与断言，不回填原始回答；同一内容会幂等复用已有任务。</p>
          <input v-model="taskQuestions[item.id]" maxlength="2000" placeholder="脱敏后的问题" />
          <input v-model="taskClaims[item.id]" maxlength="2000" placeholder="需要用户纠正的断言" />
          <select v-model="taskStatuses[item.id]" aria-label="复习证据问题类型"><option value="">选择证据问题类型</option><option value="EVIDENCE_INSUFFICIENT">证据不足</option><option value="EVIDENCE_CONFLICTING">证据冲突</option><option value="CITATION_INVALID">引用无效</option></select>
          <input v-model="taskSources[item.id]" placeholder="引用资料编号，可选：1,2" />
          <button type="button" class="task-button" @click="createReviewTask(item)">{{ taskIds[item.id] ? `已创建任务 ${taskIds[item.id]}` : "创建知识复习任务" }}</button>
          <strong>建立可重复 RAG 回归候选</strong>
          <p>只录入脱敏问题、真实资料来源名称和预期回答要点；候选创建后仍需人工激活才会运行。</p>
          <input v-model="candidateQuestions[item.id]" maxlength="2000" placeholder="回归问题" />
          <input v-model="candidateSources[item.id]" placeholder="期望来源，可选：java/collections.md,mysql/indexes.md" />
          <textarea v-model="candidatePoints[item.id]" maxlength="4000" rows="3" placeholder="每行一个预期回答要点"></textarea>
          <button type="button" class="task-button" @click="createCandidate(item)">{{ candidateIds[item.id] ? `已创建候选 ${candidateIds[item.id]}` : "创建 RAG 回归候选" }}</button>
        </div>
      </article>
    </div>
    <div v-else class="empty-state">当前筛选没有反馈。</div>
    <section class="candidate-workspace" aria-labelledby="candidate-title">
      <header><div><span>人工确认后才可运行</span><h2 id="candidate-title">RAG 回归候选</h2></div><button type="button" class="diagnose-button" @click="loadCandidates">刷新候选</button></header>
      <div v-if="candidates.length" class="feedback-list">
        <article v-for="candidate in candidates" :key="candidate.id" class="feedback-card">
          <header><strong>{{ candidate.status }}</strong><span>{{ candidate.id }}</span></header>
          <p>{{ candidate.question }}</p>
          <small>期望来源：{{ candidate.expectedSources.join("、") || "未限定" }}</small>
          <ul><li v-for="point in candidate.expectedAnswerPoints" :key="point">{{ point }}</li></ul>
          <div class="actions">
            <button v-if="candidate.status === 'PENDING'" type="button" @click="reviewCandidate(candidate, 'ACTIVE')">激活回归</button>
            <button v-if="candidate.status === 'PENDING'" type="button" class="secondary" @click="reviewCandidate(candidate, 'IGNORED')">忽略</button>
            <button v-if="candidate.status === 'ACTIVE'" type="button" @click="evaluateCandidate(candidate)">运行单题回归</button>
          </div>
          <div v-if="candidateResults[candidate.id]" class="diagnosis">
            <p>grounded：{{ candidateResults[candidate.id].grounded ? "是" : "否" }} · 来源命中：{{ candidateResults[candidate.id].expectedSourceMatched ? "是" : "否" }} · 要点覆盖：{{ candidateResults[candidate.id].matchedAnswerPoints.length }}/{{ candidateResults[candidate.id].expectedAnswerPoints.length }}</p>
            <p>策略：{{ candidateResults[candidate.id].retrievalStrategy }}{{ candidateResults[candidate.id].degraded ? ` · 已降级：${candidateResults[candidate.id].degradationReason}` : "" }}</p>
            <p>实际来源：{{ candidateResults[candidate.id].actualSources.join("、") || "无" }}</p>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">暂无人工确认的回归候选。</div>
    </section>
  </main>
</template>

<style scoped>
.feedback-workspace { display: grid; gap: var(--space-xl); margin-left: var(--workspace-rail); padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)); }
.feedback-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-lg); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.feedback-heading div { display: grid; gap: var(--space-2xs); }.feedback-heading span, .feedback-card span, .feedback-card small { color: var(--color-muted); font-size: var(--text-sm); }.feedback-heading h1 { margin: 0; color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-2xl); }
.diagnose-button { justify-self: start; min-height: 2.25rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: transparent; font: inherit; }
.diagnosis { display: grid; gap: var(--space-2xs); padding: var(--space-sm); border-left: 3px solid var(--color-accent); background: var(--color-surface); }
.diagnosis p { margin: 0; color: var(--color-muted); font-size: var(--text-xs); }
.review-task-form { display: grid; gap: var(--space-xs); padding: var(--space-md); border-left: 3px solid var(--color-accent); background: var(--color-surface); }
.review-task-form strong { color: var(--color-ink); font-size: var(--text-sm); }
.review-task-form p { color: var(--color-muted); font-size: var(--text-xs); }
.review-task-form input, .review-task-form select { width: 100%; min-height: 2.25rem; padding-inline: var(--space-sm); }
.candidate-workspace { display: grid; gap: var(--space-md); max-width: 56rem; }.candidate-workspace > header { display: flex; align-items: end; justify-content: space-between; gap: var(--space-sm); }.candidate-workspace h2 { margin: 0; color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-xl); }.candidate-workspace header span { color: var(--color-muted); font-size: var(--text-sm); }.candidate-workspace ul { margin: 0; padding-left: 1.25rem; color: var(--color-ink-2); font-size: var(--text-sm); }
.task-button { width: fit-content; min-height: 2.25rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font: inherit; font-size: var(--text-xs); font-weight: 700; }
select, textarea { border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); font: inherit; }
select { min-height: 2.5rem; padding-inline: var(--space-sm); } .feedback-list { display: grid; gap: var(--space-md); max-width: 56rem; }.feedback-card { display: grid; gap: var(--space-sm); padding: var(--space-lg); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-surface); background: var(--color-paper-2); }.feedback-card header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm); }.feedback-card p { margin: 0; color: var(--color-muted); font-size: var(--text-sm); }.feedback-card code { font-family: var(--font-mono); }.feedback-card textarea { width: 100%; padding: var(--space-sm); resize: vertical; }.actions { display: flex; gap: var(--space-sm); }.actions button { min-height: 2.5rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font: inherit; font-weight: 700; }.actions .secondary { border-color: var(--color-rule-2); color: var(--color-ink-2); background: transparent; }.danger { color: var(--color-error); }.helpful { color: var(--color-success); }.error-banner { padding: var(--space-sm) var(--space-md); color: var(--color-error); background: var(--color-error-soft); }.empty-state { min-height: 18rem; display: grid; place-items: center; color: var(--color-muted); }
@media (max-width: 68rem) { .feedback-workspace { margin-left: 0; } } @media (max-width: 42rem) { .feedback-heading { align-items: start; flex-direction: column; } }
</style>
